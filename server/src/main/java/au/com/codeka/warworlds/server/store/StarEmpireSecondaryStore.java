package au.com.codeka.warworlds.server.store;

import com.google.common.base.Preconditions;
import com.sleepycat.je.Cursor;
import com.sleepycat.je.CursorConfig;
import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.Environment;
import com.sleepycat.je.LockMode;
import com.sleepycat.je.OperationStatus;
import com.sleepycat.je.SecondaryConfig;
import com.sleepycat.je.SecondaryDatabase;
import com.sleepycat.je.SecondaryMultiKeyCreator;
import com.sleepycat.je.Transaction;

import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.annotation.Nullable;

import au.com.codeka.warworlds.common.Log;
import au.com.codeka.warworlds.common.proto.Planet;
import au.com.codeka.warworlds.common.proto.Star;

/**
 * A secondary store for stars which allows us to index them by empire (i.e. fetch all stars that
 * an empire owns).
 */
public class StarEmpireSecondaryStore {
  private static final Log log = new Log("StarEmpireSecondaryStore");

  private final SecondaryDatabase sdb;
  private final ProtobufStore<Star> stars;

  public StarEmpireSecondaryStore(Environment env, Database db, ProtobufStore<Star> stars) {
    SecondaryConfig secondaryConfig = new SecondaryConfig();
    secondaryConfig.setAllowCreate(true);
    secondaryConfig.setTransactional(true);
    secondaryConfig.setSortedDuplicates(true);
    secondaryConfig.setMultiKeyCreator(new KeyCreator());
    SecondaryDatabase sdb = env.openSecondaryDatabase(null, "empire_stars", db, secondaryConfig);

    this.sdb = Preconditions.checkNotNull(sdb);
    this.stars = Preconditions.checkNotNull(stars);
  }

  /** Gets the next {@link Star} that needs to be simulated. */
  @Nullable
  public StarIterable getStarsForEmpire(@Nullable Transaction trans, final long empireId) {
    CursorConfig cursorConfig = new CursorConfig();
    Cursor cursor = sdb.openCursor(trans, cursorConfig);
    return new StarIterable(cursor, empireId);
  }

  /** Key creator which extracts the empireIDs from stars and uses that as the key. */
  public class KeyCreator implements SecondaryMultiKeyCreator {
    @Override
    public void createSecondaryKeys(
        SecondaryDatabase secondaryDatabase,
        DatabaseEntry key,
        DatabaseEntry value,
        Set<DatabaseEntry> results) {
      if (key.getSize() != 8) {
        // it's probably not a star (probably the sequence)
        return;
      }
      Star star = stars.decodeValue(value);

      Set<Long> empireIds = new HashSet<>();
      for (Planet planet : star.planets) {
        if (planet.colony != null && planet.colony.empire_id != null) {
          long empireId = planet.colony.empire_id;
          if (!empireIds.contains(empireId)) {
            empireIds.add(empireId);
          }
        }
      }

      // TODO: fleets

      log.debug("updating empire index for star %d", star.id);
      for (long empireId : empireIds) {
        log.debug(".. adding empire %d", empireId);
        results.add(stars.encodeKey(empireId));
      }
    }
  }

  /** An {@link Iterable} for stars. */
  public class StarIterable implements Iterable<Star>, AutoCloseable {
    private final Cursor cursor;
    private final long empireId;

    public StarIterable(Cursor cursor, long empireId) {
      this.cursor = cursor;
      this.empireId = empireId;
    }

    @Override
    public void close() {
      cursor.close();
    }

    @Override
    public Iterator<Star> iterator() {
      return new StarIterator(cursor, empireId);
    }
  }

  /** An {@link Iterator} for iterating through the results of a cursor. */
  public class StarIterator implements Iterator<Star>, AutoCloseable {
    private final Cursor cursor;
    private final long empireId;

    @Nullable private DatabaseEntry currKey;
    @Nullable private DatabaseEntry currValue;
    private boolean validValue;

    StarIterator(Cursor cursor, long empireId) {
      this.cursor = Preconditions.checkNotNull(cursor);
      this.empireId = empireId;
    }

    @Override
    public boolean hasNext() {
      if (currKey == null || currValue == null) {
        currKey = stars.encodeKey(empireId);
        currValue = new DatabaseEntry();

        OperationStatus status = cursor.getSearchKey(currKey, currValue, LockMode.DEFAULT);
        validValue = status == OperationStatus.SUCCESS;
        log.debug("valid value = %s for %d", status, empireId);

      } else {
        OperationStatus status = cursor.getNextDup(currKey, currValue, LockMode.DEFAULT);
        validValue = status == OperationStatus.SUCCESS;
        log.debug("valid value = %s for %d", status, empireId);
      }
      return validValue;
    }

    @Override
    public Star next() {
      if (validValue) {
        return stars.decodeValue(currValue);
      }

      return null;
    }

    @Override
    public void close() {
      cursor.close();
    }
  }
}

package au.com.codeka.warworlds.server.net;

import java.io.OutputStream;
import java.net.Socket;

import au.com.codeka.warworlds.common.net.PacketDecoder;
import au.com.codeka.warworlds.common.proto.Account;
import au.com.codeka.warworlds.common.proto.Empire;
import au.com.codeka.warworlds.server.world.WatchableObject;

/**
 * Represents a pending connection, which is when you request /login but have not yet connected
 * to the game socket.
 */
public class PendingConnection {
  private final Account account;
  private final WatchableObject<Empire> empire;
  private final byte[] encryptionKey;

  public PendingConnection(Account account, WatchableObject<Empire> empire, byte[] encryptionKey) {
    this.account = account;
    this.empire = empire;
    this.encryptionKey = encryptionKey;
  }

  /** Called when the user actually connects to the game socket, returns a {@link Connection}. */
  public Connection connect(Socket socket, PacketDecoder decoder, OutputStream outs) {
    Connection conn = new Connection(account, empire, encryptionKey, socket, decoder, outs);
    conn.start();
    return conn;
  }

  public WatchableObject<Empire> getEmpire() {
    return empire;
  }
}

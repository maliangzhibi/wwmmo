package au.com.codeka.warworlds.client.ctrl;


import android.databinding.tool.util.Preconditions;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;

import javax.annotation.Nonnull;

import au.com.codeka.warworlds.client.world.StarCollection;
import au.com.codeka.warworlds.common.proto.Star;

/**
 * Base adapter class for adapter which show an expandable list of stars which you can expand and
 * collapse and show other things inside of.
 *
 * We assume the children we care about are all of type {@code T}.
 */
public abstract class ExpandableStarListAdapter<T> extends BaseExpandableListAdapter {
  private final StarCollection stars;

  public ExpandableStarListAdapter(@Nonnull StarCollection stars) {
    this.stars = stars;
  }

  @Override
  public boolean hasStableIds() {
    return true;
  }

  @Override
  public boolean isChildSelectable(int groupPosition, int childPosition) {
    return true;
  }

  @Override
  public int getGroupCount() {
    return stars.size();
  }

  @Override
  public Star getGroup(int groupPosition) {
    return getStar(groupPosition);
  }

  protected abstract int getNumChildren(Star star);
  protected abstract T getChild(Star star, int index);
  protected abstract View getStarView(Star star, View convertView, ViewGroup parent);
  protected abstract View getChildView(
      Star star, int index, View convertView, ViewGroup parent);
  protected abstract long getChildId(Star star, int childPosition);

  @Override
  public int getChildrenCount(int groupPosition) {
    Star star = getStar(groupPosition);
    if (star == null) {
      return 0;
    }

    return getNumChildren(star);
  }

  @Override
  public T getChild(int groupPosition, int childPosition) {
    Star star = getStar(groupPosition);
    if (star == null) {
      return null;
    }

    return getChild(star, childPosition);
  }

  @Override
  public long getGroupId(int groupPosition) {
    return stars.get(groupPosition).id;
  }

  @Override
  public long getChildId(int groupPosition, int childPosition) {
    return getChildId(stars.get(groupPosition), childPosition);
  }

  @Override
  public View getGroupView(int groupPosition, boolean isExpanded, View convertView,
      ViewGroup parent) {
    Star star = getStar(groupPosition);
    return getStarView(star, convertView, parent);
  }

  @Override
  public View getChildView(int groupPosition, final int childPosition, boolean isLastChild,
      View convertView, ViewGroup parent) {
    Star star = getStar(groupPosition);
    return getChildView(star, childPosition, convertView, parent);
  }

  protected Star getStar(int index) {
    return stars.get(index);
  }
}
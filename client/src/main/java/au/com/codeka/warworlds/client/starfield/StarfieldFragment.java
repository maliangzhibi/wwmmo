package au.com.codeka.warworlds.client.starfield;

import android.content.res.Resources;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.Button;
import android.widget.RelativeLayout;

import com.transitionseverywhere.AutoTransition;
import com.transitionseverywhere.Transition;
import com.transitionseverywhere.TransitionManager;

import au.com.codeka.warworlds.client.MainActivity;
import au.com.codeka.warworlds.client.R;
import au.com.codeka.warworlds.client.activity.BaseFragment;
import au.com.codeka.warworlds.client.activity.SharedViewHolder;
import au.com.codeka.warworlds.client.fleets.FleetListSimple;
import au.com.codeka.warworlds.client.ctrl.PlanetListSimple;
import au.com.codeka.warworlds.client.fleets.FleetsFragment;
import au.com.codeka.warworlds.client.solarsystem.SolarSystemFragment;
import au.com.codeka.warworlds.common.Log;
import au.com.codeka.warworlds.common.proto.Fleet;
import au.com.codeka.warworlds.common.proto.Planet;
import au.com.codeka.warworlds.common.proto.Star;

/**
 * This is the main fragment that shows the starfield, lets you navigate around, select stars
 * and fleets and so on.
 */
public class StarfieldFragment extends BaseFragment {
  private final Log log = new Log("StarfieldFragment");

  private ViewGroup bottomPane;
  private Button allianceBtn;

  @Override
  protected int getViewResourceId() {
    return R.layout.frag_starfield;
  }

  @Override
  public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    final SelectionDetailsView selectionDetailsView =
        (SelectionDetailsView) view.findViewById(R.id.selection_details);
    bottomPane = (ViewGroup) view.findViewById(R.id.bottom_pane);
    allianceBtn = (Button) view.findViewById(R.id.alliance_btn);

    selectionDetailsView.setHandlers(
        new PlanetListSimple.PlanetSelectedHandler() { // planetSelectHandler
          @Override
          public void onPlanetSelected(Planet planet) {
            Star star = selectionDetailsView.getStar();
            getFragmentTransitionManager().replaceFragment(
                SolarSystemFragment.class,
                SolarSystemFragment.createArguments(star.id, star.planets.indexOf(planet)),
                SharedViewHolder.builder()
                    .addSharedView(R.id.bottom_pane, "bottom_pane")
                    .build());
          }
        }, new FleetListSimple.FleetSelectedHandler() { // fleetSelectHandler
          @Override
          public void onFleetSelected(Fleet fleet) {
            Star star = selectionDetailsView.getStar();
            getFragmentTransitionManager().replaceFragment(
                FleetsFragment.class,
                FleetsFragment.createArguments(star.id, fleet.id),
                SharedViewHolder.builder()
                    .addSharedView(R.id.bottom_pane, "bottom_pane")
                    .build());
           }
        }, new View.OnClickListener() { // renameClickListener
          @Override
          public void onClick(View v) {

          }
        }, new View.OnClickListener() { // viewClickListener
          @Override
          public void onClick(View v) {
            Star star = selectionDetailsView.getStar();
            getFragmentTransitionManager().replaceFragment(
                SolarSystemFragment.class,
                SolarSystemFragment.createArguments(star.id),
                SharedViewHolder.builder()
                    .addSharedView(R.id.bottom_pane, "bottom_pane")
                    .build());
          }
        }, new View.OnClickListener() { // intelClickListener
          @Override
          public void onClick(View v) {

          }
        }, new SelectionDetailsView.ZoomToStarHandler() { // zoomToStarHandler
          @Override
          public void onZoomToStar(Star star) {

          }
        });

    StarfieldManager starfieldManager = ((MainActivity) getActivity()).getStarfieldManager();
    if (starfieldManager.getSelectedStar() != null) {
      showBottomPane();
      selectionDetailsView.showStar(starfieldManager.getSelectedStar());
    } else {
      hideBottomPane(false);
    }
    starfieldManager.setTapListener(
        new StarfieldManager.TapListener() {
      @Override
      public void onStarTapped(@Nullable Star star) {
        if (star == null) {
          hideBottomPane(false);
        } else {
          showBottomPane();
          selectionDetailsView.showStar(star);
        }
      }
    });
  }

  private void hideBottomPane(boolean instant) {
    applyBottomPaneAnimation(false, instant);
  }

  private void showBottomPane() {
    applyBottomPaneAnimation(true, false);
  }

  private boolean isPortrait() {
    return getResources().getBoolean(R.bool.is_portrait);
  }

  private void applyBottomPaneAnimation(boolean isOpen, boolean instant) {
    float dp;
    if (isPortrait()) {
      if (isOpen) {
        dp = 180;
      } else {
        dp = 34;
      }
    } else {
      if (isOpen) {
        dp = 200;
      } else {
        dp = 100;
      }
    }

    Resources r = getResources();
    float px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, r.getDisplayMetrics());

    if (!instant) {
      Transition transition = new AutoTransition()
          .setInterpolator(new AccelerateDecelerateInterpolator());
      TransitionManager.beginDelayedTransition(bottomPane, transition);
    }
    if (isPortrait()) {
      bottomPane.getLayoutParams().height = (int) px;
    } else {
      RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) allianceBtn.getLayoutParams();
      if (isOpen) {
        // NB: removeRule is not available until API level 17 :/
        lp.addRule(RelativeLayout.BELOW, 0);
        lp.addRule(RelativeLayout.ALIGN_PARENT_TOP, 1);
        lp.topMargin =
            (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 34, r.getDisplayMetrics());
      } else {
        lp.addRule(RelativeLayout.BELOW, R.id.empire_btn);
        lp.addRule(RelativeLayout.ALIGN_PARENT_TOP, 0);
        lp.topMargin =
            (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4, r.getDisplayMetrics());
      }

      bottomPane.getLayoutParams().width = (int) px;
    }
    bottomPane.requestLayout();
  }
}

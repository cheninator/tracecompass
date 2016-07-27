package org.eclipse.tracecompass.internal.provisional.tmf.ui.views.timegraph2.view.swtjfx;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.tracecompass.internal.provisional.tmf.core.views.timegraph2.model.provider.ITimeGraphModelRenderProvider;
import org.eclipse.tracecompass.internal.provisional.tmf.core.views.timegraph2.model.provider.ITimeGraphModelRenderProvider.FilterMode;
import org.eclipse.tracecompass.internal.provisional.tmf.core.views.timegraph2.model.provider.ITimeGraphModelRenderProvider.SortingMode;
import org.eclipse.tracecompass.internal.provisional.tmf.core.views.timegraph2.model.view.TimeGraphModelView;
import org.eclipse.tracecompass.internal.tmf.ui.Activator;
import org.eclipse.tracecompass.internal.tmf.ui.ITmfImageConstants;

final class ActionFactory {

    private ActionFactory() {}

    private static abstract class DrowDownMenuCreator implements IMenuCreator {

        private @Nullable Menu fMenu = null;

        @Override
        public final @Nullable Menu getMenu(@Nullable Menu parent) {
            return null; // Not used?
        }

        @Override
        public final Menu getMenu(@Nullable Control parent) {
            if (fMenu != null) {
                fMenu.dispose();
            }
            Menu menu = new Menu(parent);
            getMenuActions().forEach(action -> {
                new ActionContributionItem(action).fill(menu, -1);
            });
            fMenu = menu;
            return menu;
        }

        @Override
        public final void dispose() {
            if (fMenu != null) {
                fMenu.dispose();
                fMenu = null;
            }
        }

        protected abstract List<Action> getMenuActions();
    }

    private static class SelectSortingModeAction extends Action {

        public SelectSortingModeAction(TimeGraphModelView view) {
            super("Sorting Mode", IAction.AS_DROP_DOWN_MENU);
            ITimeGraphModelRenderProvider provider = view.getControl().getModelRenderProvider();

            setToolTipText("Select Sorting Mode");
            setImageDescriptor(Activator.getDefault().getImageDescripterFromPath(ITmfImageConstants.IMG_UI_SHOW_LEGEND));
            setMenuCreator(new DrowDownMenuCreator() {
                @Override
                protected List<Action> getMenuActions() {
                    return IntStream.range(0, provider.getSortingModes().size())
                            .mapToObj(index -> {
                                SortingMode sortingMode = provider.getSortingModes().get(index);
                                Action action = new Action(sortingMode.getName(), IAction.AS_RADIO_BUTTON) {
                                    @Override
                                    public void runWithEvent(@Nullable Event event) {
                                        if (isChecked()) {
                                            provider.setCurrentSortingMode(index);
                                            System.out.println("repainting viewer");
                                            view.getControl().repaintCurrentArea();
                                        }
                                    }
                                };
                                action.setEnabled(true);
                                action.setChecked(provider.getCurrentSortingMode() == sortingMode);
                                return action;
                            })
                            .collect(Collectors.toList());
                }
            });
        }

        @Override
        public void runWithEvent(@Nullable Event event) {
            // TODO Also open the menu, need to figure out how
        }
    }

    private static class SelectFilterModesAction extends Action {

        public SelectFilterModesAction(TimeGraphModelView view) {
            super("Filters", IAction.AS_DROP_DOWN_MENU);
            ITimeGraphModelRenderProvider provider = view.getControl().getModelRenderProvider();

            setToolTipText("Configure Filters");
            setImageDescriptor(Activator.getDefault().getImageDescripterFromPath(ITmfImageConstants.IMG_UI_FILTERS));
            setMenuCreator(new DrowDownMenuCreator() {
                @Override
                protected List<Action> getMenuActions() {
                    return IntStream.range(0, provider.getFilterModes().size())
                            .mapToObj(index -> {
                                FilterMode filterMode = provider.getFilterModes().get(index);
                                Action action = new Action(filterMode.getName(), IAction.AS_CHECK_BOX) {
                                    @Override
                                    public void runWithEvent(@Nullable Event event) {
                                        if (isChecked()) {
                                            provider.enableFilterMode(index);
                                        } else {
                                            provider.disableFilterMode(index);
                                        }
                                        System.out.println("repainting viewer");
                                        view.getControl().repaintCurrentArea();
                                    }
                                };
                                action.setEnabled(true);
                                action.setChecked(provider.getActiveFilterModes().contains(filterMode));
                                return action;
                            })
                            .collect(Collectors.toList());
                }
            });
        }

        @Override
        public void runWithEvent(@Nullable Event event) {
            // TODO Also open the menu, need to figure out how
        }
    }

    public static Action getSelectSortingModeAction(TimeGraphModelView view) {
        return new SelectSortingModeAction(view);
    }

    public static Action getSelectFilterModesAction(TimeGraphModelView view) {
        return new SelectFilterModesAction(view);
    }

}

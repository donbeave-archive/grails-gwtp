package @artifact.package@;

import com.google.inject.Inject;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;
@artifact.imports@

class @artifact.name@View extends @artifact.extends@ implements @artifact.name@Presenter.MyView {

    interface Binder extends UiBinder<Widget, @artifact.name@View> {
    }

    @UiField
    SimplePanel main;

    @Inject
    @artifact.name@View(final Binder uiBinder) {
        initWidget(uiBinder.createAndBindUi(this));
    }

    @Override
    public void setInSlot(Object slot, IsWidget content) {
        if (slot == @artifact.name@Presenter.SLOT_@artifact.name@) {
            main.setWidget(content);
        } else {
            super.setInSlot(slot, content);
        }
    }

}

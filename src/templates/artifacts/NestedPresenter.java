package @artifact.package@;

import com.google.inject.Inject;
import com.google.web.bindery.event.shared.EventBus;
import com.gwtplatform.mvp.client.Presenter;
import com.gwtplatform.mvp.client.View;
import com.gwtplatform.mvp.client.annotations.ContentSlot;
import com.google.gwt.event.shared.GwtEvent.Type;
import com.gwtplatform.mvp.client.proxy.RevealContentHandler;
@artifact.imports@

public class @artifact.name@Presenter extends Presenter<@artifact.name@Presenter.MyView, @artifact.name@Presenter.MyProxy>@artifact.presenterImplements@ {

    interface MyView extends View@artifact.viewImplements@ {
    }

@artifact.proxy@

    @ContentSlot
    public static final Type<RevealContentHandler<?>> SLOT_@artifact.name@ = new Type<RevealContentHandler<?>>();

    @Inject
    @artifact.name@Presenter(final EventBus eventBus,
            final MyView view,
            final MyProxy proxy) {
        super(eventBus, view, proxy, @artifact.revealSlot@);
        @artifact.initContent@
    }

}
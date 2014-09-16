package @artifact.package@;

import com.gwtplatform.mvp.client.gin.AbstractPresenterModule;

public class @artifact.name@Module extends AbstractPresenterModule {

    @Override
    protected void configure() {
        bindPresenter(@artifact.name@Presenter.class, @artifact.name@Presenter.MyView.class, @artifact.name@View.class, @artifact.name@Presenter.MyProxy.class);
    }

}

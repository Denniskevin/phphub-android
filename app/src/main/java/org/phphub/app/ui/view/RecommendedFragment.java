package org.phphub.app.ui.view;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.cjj.MaterialRefreshLayout;
import com.cjj.MaterialRefreshListener;
import com.kennyc.view.MultiStateView;
import com.orhanobut.logger.Logger;

import org.phphub.app.R;
import org.phphub.app.api.entity.element.Topic;
import org.phphub.app.common.adapter.TopicItemView;
import org.phphub.app.common.base.LazyFragment;
import org.phphub.app.ui.presenter.RecommendedPresenter;

import java.util.List;

import butterknife.Bind;
import io.nlopez.smartadapters.SmartAdapter;
import io.nlopez.smartadapters.adapters.RecyclerMultiAdapter;
import io.nlopez.smartadapters.utils.ViewEventListener;
import nucleus.factory.PresenterFactory;
import nucleus.factory.RequiresPresenter;
import static org.phphub.app.common.qualified.ClickType.*;
import static com.kennyc.view.MultiStateView.*;

@RequiresPresenter(RecommendedPresenter.class)
public class RecommendedFragment extends LazyFragment<RecommendedPresenter> implements
        ViewEventListener<Topic>{
    private boolean isPrepared;

    RecyclerMultiAdapter adapter;

    @Bind(R.id.multiStateView)
    MultiStateView multiStateView;

    @Bind(R.id.refresh)
    MaterialRefreshLayout refreshView;

    @Bind(R.id.recycler_view)
    RecyclerView topicListView;

    @Override
    public void onCreate(Bundle bundle) {
        final PresenterFactory<RecommendedPresenter> superFactory = super.getPresenterFactory();
        setPresenterFactory(new PresenterFactory<RecommendedPresenter>() {
            @Override
            public RecommendedPresenter createPresenter() {
                RecommendedPresenter presenter = superFactory.createPresenter();
                getApiComponent().inject(presenter);
                return presenter;
            }
        });
        super.onCreate(bundle);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.topic_normal_list, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        topicListView.setLayoutManager(new LinearLayoutManager(getActivity()));
        adapter = SmartAdapter.empty()
                .map(Topic.class, TopicItemView.class)
                .listener(this)
                .into(topicListView);

        refreshView.setMaterialRefreshListener(new MaterialRefreshListener() {
            @Override
            public void onRefresh(MaterialRefreshLayout materialRefreshLayout) {
                getPresenter().refresh();
            }

            @Override
            public void onRefreshLoadMore(MaterialRefreshLayout materialRefreshLayout) {
                super.onRefreshLoadMore(materialRefreshLayout);
                getPresenter().nextPage();
            }
        });

        isPrepared = true;
        lazyLoad();
    }

    @Override
    protected void lazyLoad() {
        if(!isPrepared || !isVisible) {
            return;
        }

        if (!canLoadData(multiStateView, adapter)) {
            return;
        }

        multiStateView.setViewState(VIEW_STATE_LOADING);
        refreshView.autoRefresh();
    }

    @Override
    protected String getTitle() {
        return getString(R.string.recommended);
    }

    public void onChangeItems(List<Topic> topics, int pageIndex) {
        if (pageIndex == 1) {
            adapter.setItems(topics);
            multiStateView.setViewState(VIEW_STATE_CONTENT);
            refreshView.finishRefresh();
        } else {
            adapter.addItems(topics);
            refreshView.finishRefreshLoadMore();
        }
    }

    public void onNetworkError(Throwable throwable, int pageIndex) {
        Logger.e(throwable.getMessage());
        if (pageIndex == 1) {
            multiStateView.setViewState(VIEW_STATE_ERROR);
        }
    }

    @Override
    public void onViewEvent(int actionId, Topic topic, int position, View view) {
        switch (actionId) {
            case CLICK_TYPE_TOPIC_CLICKED:
                navigator.navigateToTopicDetails(getActivity(), topic.getId());
                break;
        }
    }
}
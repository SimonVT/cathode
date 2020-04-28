/*
 * Copyright (C) 2017 Simon Vig Therkildsen
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.simonvt.cathode.ui.show;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import net.simonvt.cathode.R;
import net.simonvt.cathode.api.service.SyncService;
import net.simonvt.cathode.common.ui.fragment.RefreshableAppBarFragment;
import net.simonvt.cathode.common.util.DateStringUtils;
import net.simonvt.cathode.common.util.Ids;
import net.simonvt.cathode.common.util.guava.Preconditions;
import net.simonvt.cathode.databinding.HistoryFragmentBinding;
import net.simonvt.cathode.entity.Episode;
import net.simonvt.cathode.images.ImageType;
import net.simonvt.cathode.images.ImageUri;
import net.simonvt.cathode.provider.helper.EpisodeDatabaseHelper;
import net.simonvt.cathode.provider.util.DataHelper;
import net.simonvt.cathode.sync.scheduler.EpisodeTaskScheduler;
import net.simonvt.cathode.ui.CathodeViewModelFactory;

public class EpisodeHistoryFragment extends RefreshableAppBarFragment {

  private static final String TAG = "net.simonvt.cathode.ui.show.EpisodeHistoryFragment";

  private static final String ARG_EPISODEID = TAG + ".episodeId";
  private static final String ARG_SHOW_TITLE = TAG + ".showTitle";

  enum Type {
    LOADING, ERROR, EMPTY, ITEM
  }

  private Episode episode;

  private EpisodeHistoryLiveData.Result result;

  private EpisodeTaskScheduler episodeScheduler;
  private SyncService syncService;
  private EpisodeDatabaseHelper episodeHelper;

  private long episodeId;
  private String showTitle;

  private CathodeViewModelFactory viewModelFactory;
  private EpisodeHistoryViewModel viewModel;

  private HistoryFragmentBinding binding;

  public static String getTag(long episodeId) {
    return TAG + "/" + episodeId + "/history/" + Ids.newId();
  }

  public static Bundle getArgs(long episodeId, String showTitle) {
    Preconditions.checkArgument(episodeId >= 0, "episodeId must be >= 0, was " + episodeId);

    Bundle args = new Bundle();
    args.putLong(ARG_EPISODEID, episodeId);
    args.putString(ARG_SHOW_TITLE, showTitle);
    return args;
  }

  @Inject
  public EpisodeHistoryFragment(EpisodeTaskScheduler episodeScheduler, SyncService syncService,
      EpisodeDatabaseHelper episodeHelper, CathodeViewModelFactory viewModelFactory) {
    this.episodeScheduler = episodeScheduler;
    this.syncService = syncService;
    this.episodeHelper = episodeHelper;
    this.viewModelFactory = viewModelFactory;
  }

  @Override public void onCreate(@Nullable Bundle inState) {
    super.onCreate(inState);
    Bundle args = getArguments();
    episodeId = args.getLong(ARG_EPISODEID);
    showTitle = args.getString(ARG_SHOW_TITLE);
    setTitle(showTitle);

    viewModel = new ViewModelProvider(this, viewModelFactory).get(EpisodeHistoryViewModel.class);
    viewModel.setEpisodeId(episodeId);
    viewModel.getEpisode().observe(this, new Observer<Episode>() {
      @Override public void onChanged(Episode episode) {
        setEpisode(episode);
      }
    });
    viewModel.getHistory().observe(this, new Observer<EpisodeHistoryLiveData.Result>() {
      @Override public void onChanged(EpisodeHistoryLiveData.Result result) {
        setResult(result);
        setRefreshing(false);
      }
    });
  }

  @Override
  protected View createView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
      @Nullable Bundle inState) {
    binding = HistoryFragmentBinding.inflate(inflater, container, false);
    return binding.getRoot();
  }

  @Override public void onViewCreated(@NonNull View view, @Nullable Bundle inState) {
    super.onViewCreated(view, inState);
    setResult(result);
  }

  @Override public void onDestroyView() {
    binding = null;
    super.onDestroyView();
  }

  public void onRemoveHistoryItem(long historyId, int position) {
    result.getItems().remove(position);
    setResult(result);

    episodeScheduler.removeHistoryItem(episodeId, historyId, result.getItems().size() == 0);
  }

  @Override public void onRefresh() {
    viewModel.getHistory().loadData();
  }

  private void setEpisode(Episode episode) {
    this.episode = episode;

    final String title =
        DataHelper.getEpisodeTitle(requireContext(), episode.getTitle(), episode.getSeason(),
            episode.getEpisode(), episode.getWatched());

    final String firstAiredString =
        DateStringUtils.getAirdateInterval(requireContext(), episode.getFirstAired(), true);
    binding.top.topSubtitle.setText(firstAiredString);

    showTitle = episode.getShowTitle();

    setTitle(showTitle);
    binding.top.topTitle.setText(title);
    final String screenshotUri = ImageUri.create(ImageUri.ITEM_EPISODE, ImageType.STILL, episodeId);
    setBackdrop(screenshotUri);
  }

  private boolean typeOrClear(ViewGroup parent, Type type) {
    if (parent.getChildCount() > 0) {
      View firstChild = parent.getChildAt(0);
      final Type childType = (Type) firstChild.getTag();
      if (childType == type) {
        return true;
      } else {
        parent.removeAllViews();
      }
    }

    return false;
  }

  private void setResult(EpisodeHistoryLiveData.Result result) {
    this.result = result;

    if (getView() == null) {
      return;
    }

    if (result == null) {
      if (typeOrClear(binding.content.content, Type.LOADING)) {
        return;
      }

      View v = LayoutInflater.from(binding.content.content.getContext())
          .inflate(R.layout.history_progress, binding.content.content, false);
      v.setTag(Type.LOADING);
      binding.content.content.addView(v);
    } else if (!result.isSuccessful()) {
      if (typeOrClear(binding.content.content, Type.ERROR)) {
        return;
      }

      View v = LayoutInflater.from(binding.content.content.getContext())
          .inflate(R.layout.history_error, binding.content.content, false);
      v.setTag(Type.ERROR);
      binding.content.content.addView(v);
    } else if (result.getItems().size() == 0) {

      if (typeOrClear(binding.content.content, Type.EMPTY)) {
        return;
      }

      View v = LayoutInflater.from(binding.content.content.getContext())
          .inflate(R.layout.history_empty, binding.content.content, false);
      v.setTag(Type.ERROR);
      binding.content.content.addView(v);
    } else {
      List<HistoryItem> items = result.getItems();
      List<Long> ids = new ArrayList<>();
      for (HistoryItem item : items) {
        ids.add(item.historyId);
      }

      for (int i = binding.content.content.getChildCount() - 1; i >= 0; i--) {
        View v = binding.content.content.getChildAt(i);
        if (v.getTag() != Type.ITEM) {
          binding.content.content.removeViewAt(i);
          continue;
        }

        final long id = (long) v.getTag(R.id.historyId);
        if (!ids.contains(id)) {
          binding.content.content.removeViewAt(i);
        }
      }

      for (int i = 0, size = items.size(); i < size; i++) {
        final HistoryItem item = items.get(i);
        final int position = i;

        View v = null;

        if (i < binding.content.content.getChildCount()) {
          v = binding.content.content.getChildAt(i);
        }

        if (v != null) {
          final long id = (long) v.getTag(R.id.historyId);

          if (item.historyId == id) {
            TextView watchedAt = v.findViewById(R.id.watchedAt);
            View remove = v.findViewById(R.id.remove);

            watchedAt.setText(item.watchedAt);
            remove.setOnClickListener(new View.OnClickListener() {
              @Override public void onClick(View v) {
                onRemoveHistoryItem(item.historyId, position);
              }
            });

            continue;
          }
        }

        v = LayoutInflater.from(binding.content.content.getContext())
            .inflate(R.layout.history_row, binding.content.content, false);
        TextView watchedAt = v.findViewById(R.id.watchedAt);
        View remove = v.findViewById(R.id.remove);

        v.setTag(Type.ITEM);
        v.setTag(R.id.historyId, item.historyId);
        watchedAt.setText(item.watchedAt);
        remove.setOnClickListener(new View.OnClickListener() {
          @Override public void onClick(View v) {
            onRemoveHistoryItem(item.historyId, position);
          }
        });

        binding.content.content.addView(v, i);
      }
    }
  }
}

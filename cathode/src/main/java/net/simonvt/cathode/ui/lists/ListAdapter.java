/*
 * Copyright (C) 2016 Simon Vig Therkildsen
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
package net.simonvt.cathode.ui.lists;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import net.simonvt.cathode.R;
import net.simonvt.cathode.common.ui.adapter.BaseAdapter;
import net.simonvt.cathode.common.widget.OverflowView;
import net.simonvt.cathode.common.widget.RemoteImageView;
import net.simonvt.cathode.entity.ListEpisode;
import net.simonvt.cathode.entity.ListItem;
import net.simonvt.cathode.entity.ListMovie;
import net.simonvt.cathode.entity.ListPerson;
import net.simonvt.cathode.entity.ListSeason;
import net.simonvt.cathode.entity.ListShow;
import net.simonvt.cathode.images.ImageType;
import net.simonvt.cathode.images.ImageUri;
import net.simonvt.cathode.provider.util.DataHelper;

public class ListAdapter extends BaseAdapter<ListItem, ListAdapter.ListViewHolder> {

  interface ListListener {

    void onShowClick(long showId, String title, String overview);

    void onSeasonClick(long showId, long seasonId, String showTitle, int seasonNumber);

    void onEpisodeClick(long id);

    void onMovieClicked(long movieId, String title, String overview);

    void onPersonClick(long personId);

    void onRemoveItem(int position, ListItem listItem);
  }

  private static final int TYPE_SHOW = 1;
  private static final int TYPE_SEASON = 2;
  private static final int TYPE_EPISODE = 3;
  private static final int TYPE_MOVIE = 4;
  private static final int TYPE_PERSON = 5;

  ListListener listener;

  public ListAdapter(Context context, ListListener listener) {
    super(context);
    this.listener = listener;
  }

  @Override
  protected boolean areItemsTheSame(@NonNull ListItem oldItem, @NonNull ListItem newItem) {
    return oldItem.getListItemId() == newItem.getListItemId();
  }

  @Override public int getItemViewType(int position) {
    ListItem item = getList().get(position);
    switch (item.getType()) {
      case SHOW:
        return TYPE_SHOW;
      case SEASON:
        return TYPE_SEASON;
      case EPISODE:
        return TYPE_EPISODE;
      case MOVIE:
        return TYPE_MOVIE;
      case PERSON:
        return TYPE_PERSON;
      default:
        throw new IllegalStateException("Unsupported item type " + item.getType().toString());
    }
  }

  @Override public ListViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    ListViewHolder holder;

    if (viewType == TYPE_SHOW) {
      View v = LayoutInflater.from(getContext()).inflate(R.layout.row_list_show, parent, false);
      final ShowViewHolder showHolder = new ShowViewHolder(v);
      holder = showHolder;

      v.setOnClickListener(new View.OnClickListener() {
        @Override public void onClick(View v) {
          final int position = showHolder.getAdapterPosition();
          if (position != RecyclerView.NO_POSITION) {
            ListItem item = getList().get(position);
            ListShow show = item.getShow();
            listener.onShowClick(show.getId(), show.getTitle(), show.getOverview());
          }
        }
      });
    } else if (viewType == TYPE_SEASON) {
      View v = LayoutInflater.from(getContext()).inflate(R.layout.row_list_season, parent, false);
      final SeasonViewHolder seasonHolder = new SeasonViewHolder(v);
      holder = seasonHolder;

      v.setOnClickListener(new View.OnClickListener() {
        @Override public void onClick(View v) {
          final int position = seasonHolder.getAdapterPosition();
          if (position != RecyclerView.NO_POSITION) {
            ListItem item = getList().get(position);
            ListSeason season = item.getSeason();
            listener.onSeasonClick(season.getShowId(), season.getId(), season.getShowTitle(),
                season.getSeason());
          }
        }
      });
    } else if (viewType == TYPE_EPISODE) {
      View v = LayoutInflater.from(getContext()).inflate(R.layout.row_list_episode, parent, false);

      final EpisodeViewHolder episodeHolder = new EpisodeViewHolder(v);
      holder = episodeHolder;

      v.setOnClickListener(new View.OnClickListener() {
        @Override public void onClick(View v) {
          final int position = episodeHolder.getAdapterPosition();
          if (position != RecyclerView.NO_POSITION) {
            ListItem item = getList().get(position);
            listener.onEpisodeClick(item.getEpisode().getId());
          }
        }
      });
    } else if (viewType == TYPE_MOVIE) {
      View v = LayoutInflater.from(getContext()).inflate(R.layout.row_list_movie, parent, false);
      final MovieViewHolder movieHolder = new MovieViewHolder(v);
      holder = movieHolder;

      v.setOnClickListener(new View.OnClickListener() {
        @Override public void onClick(View v) {
          final int position = movieHolder.getAdapterPosition();
          if (position != RecyclerView.NO_POSITION) {
            ListItem item = getList().get(position);
            ListMovie movie = item.getMovie();
            listener.onMovieClicked(movie.getId(), movie.getTitle(), movie.getOverview());
          }
        }
      });
    } else {
      View v = LayoutInflater.from(getContext()).inflate(R.layout.row_list_person, parent, false);
      final PersonViewHolder personHolder = new PersonViewHolder(v);
      holder = personHolder;

      v.setOnClickListener(new View.OnClickListener() {
        @Override public void onClick(View v) {
          final int position = personHolder.getAdapterPosition();
          if (position != RecyclerView.NO_POSITION) {
            ListItem item = getList().get(position);
            listener.onPersonClick(item.getPerson().getId());
          }
        }
      });
    }

    final ListViewHolder finalHolder = holder;
    holder.overflow.addItem(R.id.action_list_remove, R.string.action_list_remove);
    holder.overflow.setListener(new OverflowView.OverflowActionListener() {
      @Override public void onPopupShown() {
      }

      @Override public void onPopupDismissed() {
      }

      @Override public void onActionSelected(int action) {
        final int position = finalHolder.getAdapterPosition();
        if (position != RecyclerView.NO_POSITION) {
          ListItem item = getList().get(position);
          switch (action) {
            case R.id.action_list_remove:
              listener.onRemoveItem(position, item);
              break;
          }
        }
      }
    });

    return holder;
  }

  @Override public void onViewRecycled(ListViewHolder holder) {
    holder.overflow.dismiss();
  }

  @Override public void onBindViewHolder(@NonNull ListViewHolder holder, int position) {
    ListItem item = getList().get(position);
    if (holder.getItemViewType() == TYPE_SHOW) {
      ShowViewHolder showHolder = (ShowViewHolder) holder;
      ListShow show = item.getShow();

      String poster = ImageUri.create(ImageUri.ITEM_SHOW, ImageType.POSTER, show.getId());
      showHolder.poster.setImage(poster);
      showHolder.title.setText(show.getTitle());
      showHolder.overview.setText(show.getOverview());
    } else if (holder.getItemViewType() == TYPE_SEASON) {
      SeasonViewHolder seasonHolder = (SeasonViewHolder) holder;
      ListSeason season = item.getSeason();

      String showPoster = ImageUri.create(ImageUri.ITEM_SHOW, ImageType.POSTER, season.getShowId());
      seasonHolder.poster.setImage(showPoster);
      seasonHolder.season.setText(
          getContext().getResources().getString(R.string.season_x, season.getSeason()));
      seasonHolder.show.setText(season.getShowTitle());
    } else if (holder.getItemViewType() == TYPE_EPISODE) {
      EpisodeViewHolder episodeHolder = (EpisodeViewHolder) holder;
      ListEpisode episode = item.getEpisode();

      String title =
          DataHelper.getEpisodeTitle(getContext(), episode.getTitle(), episode.getSeason(),
              episode.getEpisode(), episode.getWatched());
      String screenshotUri =
          ImageUri.create(ImageUri.ITEM_EPISODE, ImageType.STILL, episode.getId());
      episodeHolder.screen.setImage(screenshotUri);
      episodeHolder.title.setText(title);
      episodeHolder.showTitle.setText(episode.getShowTitle());
    } else if (holder.getItemViewType() == TYPE_MOVIE) {
      MovieViewHolder movieHolder = (MovieViewHolder) holder;
      ListMovie movie = item.getMovie();

      String poster = ImageUri.create(ImageUri.ITEM_MOVIE, ImageType.POSTER, movie.getId());
      movieHolder.poster.setImage(poster);
      movieHolder.title.setText(movie.getTitle());
      movieHolder.overview.setText(movie.getOverview());
    } else {
      PersonViewHolder personHolder = (PersonViewHolder) holder;
      ListPerson person = item.getPerson();

      String headshot = ImageUri.create(ImageUri.ITEM_PERSON, ImageType.PROFILE, person.getId());
      personHolder.headshot.setImage(headshot);
      personHolder.name.setText(person.getName());
    }
  }

  public static class ListViewHolder extends RecyclerView.ViewHolder {

    OverflowView overflow;

    public ListViewHolder(View v) {
      super(v);
      overflow = v.findViewById(R.id.overflow);
    }
  }

  public static class ShowViewHolder extends ListViewHolder {

    RemoteImageView poster;
    TextView title;
    TextView overview;

    public ShowViewHolder(View v) {
      super(v);
      poster = v.findViewById(R.id.poster);
      title = v.findViewById(R.id.title);
      overview = v.findViewById(R.id.overview);
    }
  }

  public static class SeasonViewHolder extends ListViewHolder {

    RemoteImageView poster;
    TextView season;
    TextView show;

    public SeasonViewHolder(View v) {
      super(v);
      poster = v.findViewById(R.id.poster);
      season = v.findViewById(R.id.season);
      show = v.findViewById(R.id.show);
    }
  }

  public static class EpisodeViewHolder extends ListViewHolder {

    RemoteImageView screen;
    TextView title;
    TextView showTitle;

    EpisodeViewHolder(View v) {
      super(v);
      screen = v.findViewById(R.id.screen);
      title = v.findViewById(R.id.title);
      showTitle = v.findViewById(R.id.showTitle);
    }
  }

  public static class MovieViewHolder extends ListViewHolder {

    public RemoteImageView poster;
    public TextView title;
    public TextView overview;

    public MovieViewHolder(View v) {
      super(v);
      poster = v.findViewById(R.id.poster);
      title = v.findViewById(R.id.title);
      overview = v.findViewById(R.id.overview);
    }
  }

  public static class PersonViewHolder extends ListViewHolder {

    RemoteImageView headshot;
    TextView name;

    public PersonViewHolder(View v) {
      super(v);
      headshot = v.findViewById(R.id.headshot);
      name = v.findViewById(R.id.person_name);
    }
  }
}

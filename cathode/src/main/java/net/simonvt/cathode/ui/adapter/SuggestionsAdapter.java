package net.simonvt.cathode.ui.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.simonvt.cathode.R;

public abstract class SuggestionsAdapter extends BaseAdapter implements Filterable {

  public static class Suggestion {

    String title;

    Long id;

    Suggestion(String title, Long id) {
      this.title = title;
      this.id = id;
    }

    public String getTitle() {
      return title;
    }

    public Long getId() {
      return id;
    }

    @Override public String toString() {
      return title;
    }
  }

  Context context;

  private Filter filter;

  List<Suggestion> suggestions;

  List<Suggestion> queries;

  List<Suggestion> known;

  protected SuggestionsAdapter(Context context) {
    this.context = context;
  }

  @Override public int getCount() {
    return suggestions == null ? 0 : suggestions.size();
  }

  @Override public Suggestion getItem(int position) {
    return suggestions.get(position);
  }

  @Override public long getItemId(int position) {
    return position;
  }

  @Override public boolean hasStableIds() {
    return false;
  }

  @Override public int getItemViewType(int position) {
    return getItem(position).id != null ? 1 : 0;
  }

  @Override public int getViewTypeCount() {
    return 2;
  }

  @Override public View getView(int position, View convertView, ViewGroup parent) {
    View v = convertView;
    Suggestion suggestion = getItem(position);
    if (v == null) {
      if (suggestion.id != null) {
        v = LayoutInflater.from(context).inflate(R.layout.suggestion_known, parent, false);
      } else {
        v = LayoutInflater.from(context).inflate(R.layout.suggestion_query, parent, false);
      }
    }

    ((TextView) v.findViewById(R.id.title)).setText(suggestion.title);

    return v;
  }

  @Override public Filter getFilter() {
    if (filter == null) {
      filter = new SearchFilter();
    }

    return filter;
  }

  private class SearchFilter extends Filter {

    @Override protected FilterResults performFiltering(CharSequence constraint) {
      FilterResults results = new FilterResults();

      if (constraint == null) return results;

      String filter = constraint.toString().toLowerCase(Locale.getDefault());

      List<Suggestion> items = new ArrayList<Suggestion>();

      int queryCount = 0;
      for (Suggestion suggestion : queries) {
        if (suggestion.title.toLowerCase(Locale.getDefault()).contains(filter)) {
          items.add(suggestion);
          queryCount++;
          if (queryCount >= 2) {
            break;
          }
        }
      }

      int knownCount = 0;
      for (Suggestion suggestion : known) {
        if (suggestion.title.toLowerCase(Locale.getDefault()).contains(filter)) {
          items.add(suggestion);
          knownCount++;
          if (knownCount >= 2) {
            break;
          }
        }
      }

      results.count = items.size();
      results.values = items;

      return results;
    }

    @Override protected void publishResults(CharSequence constraint, FilterResults results) {
      suggestions = (List<Suggestion>) results.values;

      if (results.count > 0) {
        notifyDataSetChanged();
      } else {
        notifyDataSetInvalidated();
      }
    }
  }
}

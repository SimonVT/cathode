/*
 * Copyright (C) 2018 Simon Vig Therkildsen
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

package net.simonvt.cathode.ui.credits;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import net.simonvt.cathode.api.enumeration.ItemType;

public class CreditsViewModel extends AndroidViewModel {

  private ItemType itemType;
  private long itemId;

  private LiveData<Credits> credits;

  public CreditsViewModel(@NonNull Application application) {
    super(application);
  }

  public void setItemTypeAndId(ItemType itemType, long itemId) {
    if (this.itemType == null) {
      this.itemType = itemType;
      this.itemId = itemId;

      credits = new CreditsLiveData(getApplication(), itemType, itemId);
    }
  }

  public LiveData<Credits> getCredits() {
    return credits;
  }
}

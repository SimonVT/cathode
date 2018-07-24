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

package net.simonvt.cathode.ui.person;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

public class PersonViewModel extends AndroidViewModel {

  private long personId = -1L;

  private LiveData<Person> person;

  public PersonViewModel(@NonNull Application application) {
    super(application);
  }

  public void setPersonId(long personId) {
    if (this.personId == -1L) {
      this.personId = personId;
      person = new PersonLiveData(getApplication(), personId);
    }
  }

  public LiveData<Person> getPerson() {
    return person;
  }
}

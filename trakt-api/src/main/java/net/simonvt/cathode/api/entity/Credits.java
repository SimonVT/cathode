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

package net.simonvt.cathode.api.entity;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class Credits {

  public static class Credit {

    String character;

    String job;

    Show show;

    Movie movie;

    public String getCharacter() {
      return character;
    }

    public String getJob() {
      return job;
    }

    public Show getShow() {
      return show;
    }

    public Movie getMovie() {
      return movie;
    }
  }

  public static class Crew {

    List<Credit> production;

    List<Credit> art;

    List<Credit> crew;

    @SerializedName("costume & make-up") public List<Credit> costumeAndMakeUp;

    List<Credit> directing;

    List<Credit> writing;

    List<Credit> sound;

    List<Credit> camera;

    public List<Credit> getProduction() {
      return production;
    }

    public List<Credit> getArt() {
      return art;
    }

    public List<Credit> getCrew() {
      return crew;
    }

    public List<Credit> getCostumeAndMakeUp() {
      return costumeAndMakeUp;
    }

    public List<Credit> getDirecting() {
      return directing;
    }

    public List<Credit> getWriting() {
      return writing;
    }

    public List<Credit> getSound() {
      return sound;
    }

    public List<Credit> getCamera() {
      return camera;
    }
  }

  List<Credit> cast;

  Crew crew;

  public List<Credit> getCast() {
    return cast;
  }

  public Crew getCrew() {
    return crew;
  }
}

/*
 * Copyright (C) 2014 Simon Vig Therkildsen
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

public class People {

  public static class Crew {

    List<CrewMember> production;

    List<CrewMember> art;

    List<CrewMember> crew;

    @SerializedName("costume & make-up") public List<CrewMember> costumeAndMakeUp;

    List<CrewMember> directing;

    List<CrewMember> writing;

    List<CrewMember> sound;

    List<CrewMember> camera;

    public List<CrewMember> getProduction() {
      return production;
    }

    public List<CrewMember> getArt() {
      return art;
    }

    public List<CrewMember> getCrew() {
      return crew;
    }

    public List<CrewMember> getCostumeAndMakeUp() {
      return costumeAndMakeUp;
    }

    public List<CrewMember> getDirecting() {
      return directing;
    }

    public List<CrewMember> getWriting() {
      return writing;
    }

    public List<CrewMember> getSound() {
      return sound;
    }

    public List<CrewMember> getCamera() {
      return camera;
    }
  }

  List<CastMember> cast;

  Crew crew;

  public List<CastMember> getCast() {
    return cast;
  }

  public Crew getCrew() {
    return crew;
  }
}

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

package net.simonvt.cathode.ui.person;

import java.util.ArrayList;
import java.util.List;

public class PersonCredits {

  private final List<PersonCredit> cast = new ArrayList<>();

  private final List<PersonCredit> production = new ArrayList<>();

  private final List<PersonCredit> art = new ArrayList<>();

  private final List<PersonCredit> crew = new ArrayList<>();

  private final List<PersonCredit> costumeAndMakeUp = new ArrayList<>();

  private final List<PersonCredit> directing = new ArrayList<>();

  private final List<PersonCredit> writing = new ArrayList<>();

  private final List<PersonCredit> sound = new ArrayList<>();

  private final List<PersonCredit> camera = new ArrayList<>();

  public void addCast(List<PersonCredit> cast) {
    if (cast != null) {
      this.cast.addAll(cast);
    }
  }

  public void addProduction(List<PersonCredit> production) {
    if (production != null) {
      this.production.addAll(production);
    }
  }

  public void addArt(List<PersonCredit> art) {
    if (art != null) {
      this.art.addAll(art);
    }
  }

  public void addCrew(List<PersonCredit> crew) {
    if (crew != null) {
      this.crew.addAll(crew);
    }
  }

  public void addCostumeAndMakeUp(List<PersonCredit> costumeAndMakeUp) {
    if (costumeAndMakeUp != null) {
      this.costumeAndMakeUp.addAll(costumeAndMakeUp);
    }
  }

  public void addDirecting(List<PersonCredit> directing) {
    if (directing != null) {
      this.directing.addAll(directing);
    }
  }

  public void addWriting(List<PersonCredit> writing) {
    if (writing != null) {
      this.writing.addAll(writing);
    }
  }

  public void addSound(List<PersonCredit> sound) {
    if (sound != null) {
      this.sound.addAll(sound);
    }
  }

  public void addCamera(List<PersonCredit> camera) {
    if (camera != null) {
      this.camera.addAll(camera);
    }
  }

  public List<PersonCredit> getCast() {
    return cast;
  }

  public List<PersonCredit> getProduction() {
    return production;
  }

  public List<PersonCredit> getArt() {
    return art;
  }

  public List<PersonCredit> getCrew() {
    return crew;
  }

  public List<PersonCredit> getCostumeAndMakeUp() {
    return costumeAndMakeUp;
  }

  public List<PersonCredit> getDirecting() {
    return directing;
  }

  public List<PersonCredit> getWriting() {
    return writing;
  }

  public List<PersonCredit> getSound() {
    return sound;
  }

  public List<PersonCredit> getCamera() {
    return camera;
  }
}

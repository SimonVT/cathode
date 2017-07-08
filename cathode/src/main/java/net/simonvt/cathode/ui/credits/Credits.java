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
package net.simonvt.cathode.ui.credits;

import java.util.List;

public class Credits {

  private List<Credit> cast;

  private List<Credit> production;

  private List<Credit> art;

  private List<Credit> crew;

  private List<Credit> costumeAndMakeUp;

  private List<Credit> directing;

  private List<Credit> writing;

  private List<Credit> sound;

  private List<Credit> camera;

  public Credits(List<Credit> cast, List<Credit> production, List<Credit> art, List<Credit> crew,
      List<Credit> costumeAndMakeUp, List<Credit> directing, List<Credit> writing,
      List<Credit> sound, List<Credit> camera) {
    this.cast = cast;
    this.production = production;
    this.art = art;
    this.crew = crew;
    this.costumeAndMakeUp = costumeAndMakeUp;
    this.directing = directing;
    this.writing = writing;
    this.sound = sound;
    this.camera = camera;
  }

  public List<Credit> getCast() {
    return cast;
  }

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

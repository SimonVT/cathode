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

public class Rating {

  public static class Distribution {

    @SerializedName("1") Integer one;

    @SerializedName("2") Integer two;

    @SerializedName("3") Integer three;

    @SerializedName("4") Integer four;

    @SerializedName("5") Integer five;

    @SerializedName("6") Integer six;

    @SerializedName("7") Integer seven;

    @SerializedName("8") Integer eight;

    @SerializedName("9") Integer nine;

    @SerializedName("10") Integer ten;

    public Integer getOne() {
      return one;
    }

    public Integer getTwo() {
      return two;
    }

    public Integer getThree() {
      return three;
    }

    public Integer getFour() {
      return four;
    }

    public Integer getFive() {
      return five;
    }

    public Integer getSix() {
      return six;
    }

    public Integer getSeven() {
      return seven;
    }

    public Integer getEight() {
      return eight;
    }

    public Integer getNine() {
      return nine;
    }

    public Integer getTen() {
      return ten;
    }
  }

  Integer rating;

  Integer votes;

  Distribution distribution;

  public Integer getRating() {
    return rating;
  }

  public Integer getVotes() {
    return votes;
  }

  public Distribution getDistribution() {
    return distribution;
  }
}

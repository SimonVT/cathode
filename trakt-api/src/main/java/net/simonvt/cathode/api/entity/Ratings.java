package net.simonvt.cathode.api.entity;

import com.google.gson.annotations.SerializedName;

public class Ratings {

  public class Distribution {

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

  private Integer percentage;

  private Integer votes;

  private Integer loved;

  private Integer hated;

  private Distribution distribution;

  public Integer getPercentage() {
    return percentage;
  }

  public Integer getVotes() {
    return votes;
  }

  public Integer getLoved() {
    return loved;
  }

  public Integer getHated() {
    return hated;
  }

  public Distribution getDistribution() {
    return distribution;
  }
}

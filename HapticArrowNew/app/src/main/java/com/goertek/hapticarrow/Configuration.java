package com.goertek.hapticarrow;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Created by dell on 2016/10/26.
 */

public class Configuration{
    @SerializedName("arrow_prepare")
    List<Integer> arrowPrepareId;
    @SerializedName("arrow_out_heavy")
    List<Integer> arrowOutHeavyId;
    @SerializedName("arrow_out_light")
    List<Integer> arrowOutLightId;
    @SerializedName("arrow_fly_heavy")
    List<Integer> arrowFlyHeavyId;
    @SerializedName("arrow_fly_light")
    List<Integer> arrowFlyLightId;
    @SerializedName("arrow_end")
    List<Integer> arrowEndId;
}

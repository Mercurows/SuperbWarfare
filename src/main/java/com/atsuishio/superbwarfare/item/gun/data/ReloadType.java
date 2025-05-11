package com.atsuishio.superbwarfare.item.gun.data;

import com.google.gson.annotations.SerializedName;

public enum ReloadType {
    @SerializedName("Magazine")
    MAGAZINE,
    @SerializedName("Clip")
    CLIP,
    @SerializedName("Iterative")
    ITERATIVE,
}

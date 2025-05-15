package com.atsuishio.superbwarfare.data.gun;

import com.google.gson.annotations.SerializedName;

public enum ReloadType {
    @SerializedName("Magazine")
    MAGAZINE,
    @SerializedName("Clip")
    CLIP,
    @SerializedName("Iterative")
    ITERATIVE,
}

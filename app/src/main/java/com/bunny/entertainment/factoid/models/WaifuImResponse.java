package com.bunny.entertainment.factoid.models;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class WaifuImResponse {
    @SerializedName("images")
    private List<WaifuImage> images;

    public String getImageUrl() {
        if (images != null && !images.isEmpty()) {
            return images.get(0).getUrl();
        }
        return null;
    }

    private static class WaifuImage {
        @SerializedName("url")
        private String url;

        public String getUrl() {
            return url;
        }
    }
}
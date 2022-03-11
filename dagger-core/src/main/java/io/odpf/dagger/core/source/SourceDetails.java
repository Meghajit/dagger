package io.odpf.dagger.core.source;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;

import static io.odpf.dagger.core.utils.Constants.STREAM_SOURCE_DETAILS_SOURCE_NAME_KEY;
import static io.odpf.dagger.core.utils.Constants.STREAM_SOURCE_DETAILS_SOURCE_TYPE_KEY;

public class SourceDetails {
    @SerializedName(STREAM_SOURCE_DETAILS_SOURCE_NAME_KEY)
    @Getter
    private SourceName sourceName;

    @SerializedName(STREAM_SOURCE_DETAILS_SOURCE_TYPE_KEY)
    @Getter
    private SourceType sourceType;

    public SourceDetails(SourceName sourceName, SourceType sourceType) {
        this.sourceName = sourceName;
        this.sourceType = sourceType;
    }
}
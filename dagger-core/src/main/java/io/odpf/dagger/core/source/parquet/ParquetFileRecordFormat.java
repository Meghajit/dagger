package io.odpf.dagger.core.source.parquet;

import static com.google.api.client.util.Preconditions.checkArgument;

import io.odpf.dagger.core.source.parquet.reader.ReaderProvider;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.connector.file.src.reader.FileRecordFormat;
import org.apache.flink.core.fs.Path;
import org.apache.flink.types.Row;

import java.util.function.Supplier;

public class ParquetFileRecordFormat implements FileRecordFormat<Row> {
    private final ReaderProvider parquetFileReaderProvider;
    private final Supplier<TypeInformation<Row>> typeInformationProvider;

    private ParquetFileRecordFormat(ReaderProvider parquetFileReaderProvider, Supplier<TypeInformation<Row>> typeInformationProvider) {
        this.parquetFileReaderProvider = parquetFileReaderProvider;
        this.typeInformationProvider = typeInformationProvider;
    }

    @Override
    public Reader<Row> createReader(Configuration config, Path filePath, long splitOffset, long splitLength) {
        return parquetFileReaderProvider.getReader(filePath.toString());
    }

    /* TO DO: Need to implement a way on how to re-create the reader from saved state or from checkpoint */
    @Override
    public Reader<Row> restoreReader(Configuration config, Path filePath, long restoredOffset, long splitOffset, long splitLength) {
        throw new UnsupportedOperationException("Error: Restoring a reader from saved state is not implemented yet");
    }

    @Override
    public boolean isSplittable() {
        return false;
    }

    @Override
    public TypeInformation<Row> getProducedType() {
        return typeInformationProvider.get();
    }

    public static class Builder {
        private ReaderProvider parquetFileReaderProvider;
        private Supplier<TypeInformation<Row>> typeInformationProvider;

        public static Builder getInstance() {
            return new Builder();
        }

        private Builder() {
            this.parquetFileReaderProvider = null;
            this.typeInformationProvider = null;
        }

        public Builder setParquetFileReaderProvider(ReaderProvider parquetFileReaderProvider) {
            this.parquetFileReaderProvider = parquetFileReaderProvider;
            return this;
        }

        public Builder setTypeInformationProvider(Supplier<TypeInformation<Row>> typeInformationProvider) {
            this.typeInformationProvider = typeInformationProvider;
            return this;
        }

        public ParquetFileRecordFormat build() {
            checkArgument(parquetFileReaderProvider != null, "ReaderProvider is required but is set as null");
            checkArgument(typeInformationProvider != null, "TypeInformationProvider is required but is set as null");
            return new ParquetFileRecordFormat(parquetFileReaderProvider, typeInformationProvider);
        }
    }
}

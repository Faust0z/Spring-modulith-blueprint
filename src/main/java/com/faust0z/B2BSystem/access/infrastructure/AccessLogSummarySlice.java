package com.faust0z.B2BSystem.access.infrastructure;

import com.faust0z.B2BSystem.access.domain.AccessLogSummary;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;

import java.util.Iterator;
import java.util.List;
import java.util.function.Function;

public class AccessLogSummarySlice implements Slice<AccessLogSummary> {

    private final List<AccessLogSummary> content;
    private final Pageable pageable;
    private final boolean hasNext;

    public AccessLogSummarySlice(List<AccessLogSummary> content, Pageable pageable, boolean hasNext) {
        this.content = content;
        this.pageable = pageable;
        this.hasNext = hasNext;
    }

    @Override
    public boolean hasNext() {
        return hasNext;
    }

    @Override
    public boolean hasContent() {
        return !content.isEmpty();
    }

    @Override
    public List<AccessLogSummary> getContent() {
        return content;
    }

    @Override
    public int getNumber() {
        return pageable.getPageNumber();
    }

    @Override
    public int getSize() {
        return pageable.getPageSize();
    }

    @Override
    public int getNumberOfElements() {
        return content.size();
    }

    @Override
    public Pageable getPageable() {
        return pageable;
    }

    @Override
    public Sort getSort() {
        return pageable.getSort();
    }

    @Override
    public boolean isFirst() {
        return pageable.getPageNumber() == 0;
    }

    @Override
    public boolean isLast() {
        return !hasNext;
    }

    @Override
    public boolean hasPrevious() {
        return pageable.getPageNumber() > 0;
    }

    @Override
    public Pageable nextPageable() {
        return hasNext ? pageable.next() : pageable;
    }

    @Override
    public Pageable previousPageable() {
        return pageable.previousOrFirst();
    }

    @Override
    public Iterator<AccessLogSummary> iterator() {
        return content.iterator();
    }

    @SuppressWarnings("unchecked")
    @Override
    public <U> Slice<U> map(Function<? super AccessLogSummary, ? extends U> mapper) {
        List<U> mapped = (List<U>) content.stream().map(mapper).toList();
        return new AccessLogSummarySliceU<>(mapped, pageable, hasNext);
    }

    private static class AccessLogSummarySliceU<U> implements Slice<U> {

        private final List<U> content;
        private final Pageable pageable;
        private final boolean hasNext;

        AccessLogSummarySliceU(List<U> content, Pageable pageable, boolean hasNext) {
            this.content = content;
            this.pageable = pageable;
            this.hasNext = hasNext;
        }

        @Override
        public boolean hasNext() {
            return hasNext;
        }

        @Override
        public boolean hasContent() {
            return !content.isEmpty();
        }

        @Override
        public List<U> getContent() {
            return content;
        }

        @Override
        public int getNumber() {
            return pageable.getPageNumber();
        }

        @Override
        public int getSize() {
            return pageable.getPageSize();
        }

        @Override
        public int getNumberOfElements() {
            return content.size();
        }

        @Override
        public Pageable getPageable() {
            return pageable;
        }

        @Override
        public Sort getSort() {
            return pageable.getSort();
        }

        @Override
        public boolean isFirst() {
            return pageable.getPageNumber() == 0;
        }

        @Override
        public boolean isLast() {
            return !hasNext;
        }

        @Override
        public boolean hasPrevious() {
            return pageable.getPageNumber() > 0;
        }

        @Override
        public Pageable nextPageable() {
            return hasNext ? pageable.next() : pageable;
        }

        @Override
        public Pageable previousPageable() {
            return pageable.previousOrFirst();
        }

        @Override
        public Iterator<U> iterator() {
            return content.iterator();
        }

        @Override
        public <V> Slice<V> map(Function<? super U, ? extends V> mapper) {
            throw new UnsupportedOperationException();
        }
    }
}
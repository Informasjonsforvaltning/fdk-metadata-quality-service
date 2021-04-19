package no.fdk.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuple3;

import java.util.List;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class PaginationUtils {

    public static <T> Page<T> toPage(Tuple2<List<T>, Long> tuple) {
        return toPage(tuple.getT1(), PageRequest.of(0, Math.toIntExact(tuple.getT2())), tuple.getT2());
    }

    public static <T> Page<T> toPage(Tuple3<List<T>, Pageable, Long> tuple) {
        return toPage(tuple.getT1(), tuple.getT2(), tuple.getT3());
    }

    public static <T> Page<T> toPage(List<T> items, Long count) {
        return toPage(items, PageRequest.of(0, Math.toIntExact(count)), count);
    }

    public static <T> Page<T> toPage(List<T> items, Pageable pageable, Long count) {
        return new PageImpl<>(items, pageable, count);
    }

}

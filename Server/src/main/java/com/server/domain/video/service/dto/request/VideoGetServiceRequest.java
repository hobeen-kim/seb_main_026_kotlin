package com.server.domain.video.service.dto.request;

import com.server.domain.video.repository.dto.request.VideoGetDataRequest;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.springframework.data.domain.PageRequest;

@AllArgsConstructor
@Builder
@Getter
public class VideoGetServiceRequest {

    private Long loginMemberId;

    private int page;

    private int size;
    private String categoryName;
    private String sort;
    private boolean subscribe;
    private Boolean free;
    private boolean isPurchased;
    public VideoGetDataRequest toDataRequest() {
        return VideoGetDataRequest.builder()
                .loginMemberId(loginMemberId)
                .pageable(PageRequest.of(page, size))
                .categoryName(categoryName)
                .sort(sort)
                .subscribe(subscribe)
                .free(free)
                .isPurchased(isPurchased)
                .build();
    }

    public boolean isDefault() {
        return size == 16 && !subscribe && free == null && isPurchased;
    }

    @Override
    public String toString() {
        return "VideoGetServiceRequest{" +
                "loginMemberId=" + loginMemberId +
                ", page=" + page +
                ", categoryName='" + categoryName + '\'' +
                ", sort='" + sort + '\'' +
                '}';
    }
}

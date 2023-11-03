package com.server.domain.video.service.dto.response;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.server.domain.video.entity.Video;
import com.server.global.reponse.RestPage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@AllArgsConstructor
@Getter
@Builder
@NoArgsConstructor
public class VideoPageResponse {

    private Long videoId;
    private String videoName;
    private String thumbnailUrl;
    private Integer views;
    private Integer price;
    private Float star;
    private Boolean isPurchased;
    private Boolean isInCart;
    private String description;
    private List<VideoCategoryResponse> categories;
    private VideoChannelResponse channel;
    @JsonSerialize(using = ToStringSerializer.class)
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    private LocalDateTime createdDate;

    public static RestPage<VideoPageResponse> of(
            Page<Video> videos,
            List<Boolean> isPurchaseInOrder,
            List<Boolean> isSubscribeInOrder,
            List<Map<String, String>> urlsInOrder,
            List<Long> videoIdsInCart) {

        List<VideoPageResponse> videoResponses = new ArrayList<>();

        for (int i = 0; i < videos.getContent().size(); i++) {
            VideoPageResponse videoResponse = of(videos.getContent().get(i),
                    isPurchaseInOrder.get(i),
                    isSubscribeInOrder.get(i),
                    urlsInOrder.get(i),
                    videoIdsInCart.contains(videos.getContent().get(i).getVideoId()));

            videoResponses.add(videoResponse);
        }

        return new RestPage<>(videoResponses, videos.getPageable(), videos.getTotalElements());
    }

    private static VideoPageResponse of(Video video,
                                        boolean isPurchased,
                                        boolean isSubscribed,
                                        Map<String, String> urls,
                                        boolean isInCart) {
        return VideoPageResponse.builder()
                .videoId(video.getVideoId())
                .videoName(video.getVideoName())
                .thumbnailUrl(urls.get("thumbnailUrl"))
                .views(video.getView())
                .price(video.getPrice())
                .star(video.getStar())
                .isPurchased(isPurchased)
                .isInCart(isInCart)
                .description(video.getDescription())
                .categories(VideoCategoryResponse.of(video.getVideoCategories()))
                .channel(VideoChannelResponse.of(video.getChannel(), isSubscribed, urls.get("imageUrl")))
                .createdDate(video.getCreatedDate())
                .build();
    }
}

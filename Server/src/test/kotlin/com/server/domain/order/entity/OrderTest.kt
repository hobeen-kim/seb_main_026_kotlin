package com.server.domain.order.entity

import com.server.domain.member.entity.Member
import com.server.domain.video.entity.Video
import com.server.global.exception.businessexception.orderexception.*
import org.assertj.core.api.Assertions.*
import org.assertj.core.groups.Tuple
import org.junit.jupiter.api.*
import org.junit.jupiter.api.DynamicTest.*
import org.junit.jupiter.api.function.Executable
import java.time.LocalDateTime

class OrderTest {

    @Test
    @DisplayName("member, video, reward 를 받아 order 를 생성한다.")
    fun createOrder() {
        //given
        val member = Member.builder()
            .reward(500)
            .build()

        val video1 = Video.builder()
            .price(500)
            .build()

        val video2 = Video.builder()
            .price(500)
            .build()

        val usingReward = 500
        val totalPayAmount = video1.price + video2.price - usingReward

        //when
        val order = Order.createOrder(member, listOf(video1, video2), usingReward)

        //then
        assertThat(order.totalPayAmount).isEqualTo(totalPayAmount)
        assertThat(order.remainRefundAmount).isEqualTo(0)
        assertThat(order.reward).isEqualTo(usingReward)
        assertThat(order.remainRefundReward).isEqualTo(0)
        assertThat(order.member).isEqualTo(member)
        assertThat(order.orderStatus).isEqualTo(OrderStatus.ORDERED)
        assertThat(order.orderVideos).hasSize(2)
            .extracting("video", "orderStatus")
            .containsExactlyInAnyOrder(
                Tuple.tuple(video1, OrderStatus.ORDERED),
                Tuple.tuple(video2, OrderStatus.ORDERED)
            )
    }

    @Test
    @DisplayName("order 생성 시 member 의 reward 가 부족하면 RewardNotEnoughException 이 발생한다.")
    fun createOrderRewardNotEnoughException() {
        //given
        val member = Member()

        val video1 = Video.builder()
            .price(500)
            .build()

        val video2 = Video.builder()
            .price(500)
            .build()

        val usingReward = 500

        //when && then
        assertThatThrownBy { Order.createOrder(member, listOf(video1, video2), usingReward) }
            .isInstanceOf(RewardNotEnoughException::class.java)
    }

    @Test
    @DisplayName("order 생성 시 totalAmount 보다 사용하는 reward 가 크면 RewardExceedException 이 발생한다.")
    fun createOrderRewardExceedException() {
        //given
        val member = Member.builder()
            .reward(500)
            .build()

        val video1 = Video.builder()
            .price(100)
            .build()

        val video2 = Video.builder()
            .price(100)
            .build()

        val usingReward = 500

        //when && then
        assertThatThrownBy { Order.createOrder(member, listOf(video1, video2), usingReward) }
            .isInstanceOf(RewardExceedException::class.java)
            .hasMessage("주문 금액보다 reward 가 초과합니다.")
    }

    @Test
    @DisplayName("order 의 비디오 목록을 가져온다.")
    fun getVideos() {
        //given
        val member = Member.builder()
            .build()

        val video1 = Video.builder()
            .videoName("title1")
            .build()

        val video2 = Video.builder()
            .videoName("title2")
            .build()

        val order = Order.createOrder(member, listOf(video1, video2), 0)

        //when
        val videos = order.videos

        //then
        assertThat(videos).hasSize(2)
            .extracting("videoName")
            .containsExactlyInAnyOrder(
                "title1", "title2"
            )
    }

    @Test
    @DisplayName("order 가 주문완료되기에 유효한 상태인지 확인한다.")
    fun checkValidOrder() {
        //given
        val member = Member.builder()
            .build()

        val video1 = Video.builder()
            .build()

        val video2 = Video.builder()
            .build()

        val payment = video1.price + video2.price

        val order = Order.createOrder(member, listOf(video1, video2), 0)

        //when & then
        assertThatNoException().isThrownBy { order.checkValidOrder(payment) }
    }

    @Test
    @DisplayName("order 가 유효한 상태인지 확인할 때 취소된 상태이면 OrderNotValidException 이 발생한다.")
    fun checkValidOrderIsCanceled() {
        //given
        val member = Member()
        val video1 = Video.builder().build()
        val video2 = Video.builder().build()

        val payAmount = video1.price + video2.price
        val order = Order.createOrder(member, listOf(video1, video2), 0)
        order.cancelAllOrder()

        //when & then
        assertThatThrownBy { order.checkValidOrder(payAmount) }
            .isInstanceOf(OrderNotValidException::class.java)
            .hasMessage("유효한 주문이 아닙니다.")
    }

    @Test
    @DisplayName("order 가 유효한 상태인지 확인할 때 최초에 요청했던 값과 맞지 않으면 PriceNotMatchException 이 발생한다.")
    fun checkValidOrderPriceNotMatchException() {
        //given
        val member = Member()
        val video1 = Video.builder().build()
        val video2 = Video.builder().build()

        val payAmount = video1.price + video2.price
        val order = Order.createOrder(member, listOf(video1, video2), 0)

        //when & then
        assertThatThrownBy { order.checkValidOrder(payAmount + 100) }
            .isInstanceOf(PriceNotMatchException::class.java)
            .hasMessage("요청한 가격과 다릅니다.")
    }

    @Test
    @DisplayName("ORDERED 상태인 Order 을 주문완료(COMPLETED) 상태로 만든다.")
    fun completeOrder() {
        //given
        val member = Member.builder()
            .reward(500)
            .build()

        val beforeOrderReward = member.reward

        val video1 = Video.builder()
            .price(500)
            .build()

        val video2 = Video.builder()
            .price(500)
            .build()

        val useReward = 500
        val payAmount = video1.price + video2.price - useReward
        val orderDate = LocalDateTime.now()
        val order = Order.createOrder(member, listOf(video1, video2), useReward)

        //when
        order.completeOrder(orderDate, "paymentKey")

        //then

        //then
        Assertions.assertAll("order 상태가 completed 로 바뀐다.",
            Executable { assertThat(order.remainRefundAmount).isEqualTo(payAmount) },
            Executable { assertThat(order.remainRefundReward).isEqualTo(useReward) },
            Executable { assertThat(order.orderStatus).isEqualTo(OrderStatus.COMPLETED) },
            Executable { assertThat(order.paymentKey).isEqualTo("paymentKey") },
            Executable { assertThat(order.completedDate).isEqualTo(orderDate) }
        )

        Assertions.assertAll("orderVideo 의 상태가 completed 로 바뀐다.",
            Executable {
                assertThat(order.orderVideos)
                    .hasSize(2)
                    .extracting("orderStatus").containsExactly(OrderStatus.COMPLETED, OrderStatus.COMPLETED)
            }
        )

        Assertions.assertAll("member 의 리워드가 차감된다.",
            Executable { assertThat(member.reward).isEqualTo(beforeOrderReward - useReward) }
        )
    }

    @Test
    @DisplayName("ORDERED 상태인 Order 을 주문완료(COMPLETED) 상태로 만들 때 처음에 요청했던 reward 만큼 보유하지 않으면 RewardNotEnoughException 이 발생한다.")
    fun completeOrderRewardNotEnoughException() {
        //given
        val member = Member.builder()
            .reward(500)
            .build()

        val video1 = Video.builder()
            .price(500)
            .build()

        val video2 = Video.builder()
            .price(500)
            .build()

        val useReward = 500
        val orderDate = LocalDateTime.now()
        val order = Order.createOrder(member, listOf(video1, video2), useReward)

        member.minusReward(member.reward) // member 의 reward 를 0 으로 만든다.

        //when & then
        assertThatThrownBy { order.completeOrder(orderDate, "paymentKey") }
            .isInstanceOf(RewardNotEnoughException::class.java)
            .hasMessage("reward 가 부족합니다.")
    }

    @Test
    @DisplayName("order 내의 모든 orderVideo, order 를 취소 상태로 변경한다.")
    fun cancelAllOrder() {
        //given
        val member = Member.builder()
            .reward(500)
            .build()

        val video1 = Video.builder()
            .price(500)
            .build()

        val video2 = Video.builder()
            .price(500)
            .build()

        val useReward = 500
        val order = Order.createOrder(member, listOf(video1, video2), useReward)

        //when
        val refund = order.cancelAllOrder()

        //then

        //then
        Assertions.assertAll("orderVideo 의 상태가 canceled 로 바뀐다.",
            Executable {
                assertThat(order.orderVideos)
                    .hasSize(2)
                    .extracting("orderStatus").containsExactly(OrderStatus.CANCELED, OrderStatus.CANCELED)
            }
        )

        Assertions.assertAll("order 의 상태가 canceled 로 바뀐다.",
            Executable {
                assertThat(order.orderStatus).isEqualTo(OrderStatus.CANCELED)
            }
        )
    }

    @Test
    @DisplayName("order 을 취소하면 환불할 금액과 리워드를 반환하고 남은 금액을 0으로 만든다.")
    fun cancelAllOrderReturnValue() {
        //given
        val member = Member.builder()
            .reward(500)
            .build()

        val video1 = Video.builder()
            .price(500)
            .build()

        val video2 = Video.builder()
            .price(500)
            .build()

        val useReward = 500
        val order = Order.createOrder(member, listOf(video1, video2), useReward)
        order.completeOrder(LocalDateTime.now(), "paymentKey")

        //when
        val refund = order.cancelAllOrder()

        //then
        Assertions.assertAll("환불할 금액과 리워드를 반환한다.",
            Executable {
                assertThat(refund.refundAmount).isEqualTo(video1.price + video2.price - useReward)
            },
            Executable {
                assertThat(refund.refundReward).isEqualTo(useReward)
            }
        )

        Assertions.assertAll("남은 금액을 0으로 만든다.",
            Executable {
                assertThat(order.remainRefundAmount).isEqualTo(0)
            },
            Executable {
                assertThat(order.remainRefundReward).isEqualTo(0)
            }
        )
    }

    @Test
    @DisplayName("order 을 취소하면 남은 리워드 만큼 member Reward 가 반환된다.")
    fun cancelAllOrderRefundReward() {
        //given
        val member = Member.builder()
            .reward(500)
            .build()

        val video1 = Video.builder()
            .price(500)
            .build()

        val video2 = Video.builder()
            .price(500)
            .build()

        val useReward = 500

        val order = Order.createOrder(member, listOf(video1, video2), useReward)
        order.completeOrder(LocalDateTime.now(), "paymentKey")

        val afterOrderRemainReward = member.reward

        //when
        order.cancelAllOrder()

        //then
        assertThat(member.reward).isEqualTo(afterOrderRemainReward + useReward)
    }

    @Test
    @DisplayName("order 가 completed 상태인지 확인한다. - true")
    fun isCompleteTrue() {
        //given
        val member = Member.builder()
            .build()

        val video1 = Video.builder()
            .build()

        val video2 = Video.builder()
            .build()

        val order = Order.createOrder(member, listOf(video1, video2), 0)
        order.completeOrder(LocalDateTime.now(), "paymentKey")

        //when
        val isComplete = order.isComplete

        //then
        assertThat(isComplete).isTrue()
    }

    @Test
    @DisplayName("order 가 completed 상태인지 확인한다. - false")
    fun isCompleteFalse() {
        //given
        val member = Member.builder()
            .build()

        val video1 = Video.builder()
            .build()

        val video2 = Video.builder()
            .build()

        val order = Order.createOrder(member, listOf(video1, video2), 0)

        //when
        val isComplete = order.isComplete

        //then
        assertThat(isComplete).isFalse()
    }

    @Test
    @DisplayName("order 의 상태를 확인하고 canceled 상태이면 OrderAlreadyCanceledException 이 발생한다.")
    fun checkAlreadyCanceled() {
        //given
        val member = Member.builder()
            .build()

        val video1 = Video.builder()
            .build()

        val video2 = Video.builder()
            .build()

        val order = Order.createOrder(member, listOf(video1, video2), 0)
        order.cancelAllOrder()

        //when & then
        assertThatThrownBy { order.checkAlreadyCanceled() }
            .isInstanceOf(OrderAlreadyCanceledException::class.java)
            .hasMessage("이미 취소된 주문입니다.")
    }

    @TestFactory
    @DisplayName("order 의 상태를 확인하고 canceled 상태가 아니면 예외가 발생하지 않는다.")
    fun checkAlreadyCanceledNotCanceled(): Collection<DynamicTest> {
        //given
        val member = Member.builder()
            .build()

        val video1 = Video.builder().build()

        return listOf(
            dynamicTest("order 의 상태가 ordered 이면 예외가 발생하지 않는다.") {
                //given
                val order = Order.createOrder(member, listOf(video1), 0)

                //when & then
                assertThatNoException()
                    .isThrownBy { order.checkAlreadyCanceled() }
            },
            dynamicTest("order 의 상태가 completed 이면 예외가 발생하지 않는다.") {
                //given
                val order = Order.createOrder(member, listOf(video1), 0)

                //when & then
                assertThatNoException()
                    .isThrownBy { order.checkAlreadyCanceled() }
            }
        )
    }

    @Test
    @DisplayName("orderVideo 를 취소하면 상태가 취소 상태로 변경한다.")
    fun cancelVideoOrder() {
        //given
        val member = Member.builder().build()
        val video1 = Video.builder().build()
        val video2 = Video.builder().build()

        val order = Order.createOrder(member, listOf(video1, video2), 0)
        val orderVideo = getOrderVideo(order, video1)

        //when
        order.cancelVideoOrder(orderVideo)

        //then
        assertThat(orderVideo.orderStatus).isEqualTo(OrderStatus.CANCELED)
    }

    @TestFactory
    @DisplayName("orderVideo 를 취소하면 환불할 금액과 리워드를 반환한다.")
    fun cancelVideoOrderReturnValue(): Collection<DynamicTest> {
        //given
        val member = Member.builder().reward(1500).build()
        val video1 = Video.builder().price(1000).build()
        val video2 = Video.builder().price(1000).build()

        val userReward = 1500

        val order = Order.createOrder(member, listOf(video1, video2), userReward)
        order.completeOrder(LocalDateTime.now(), "paymentKey")

        return listOf(
            dynamicTest("video1 을 취소한다.") {
                //given
                val orderVideo1 = getOrderVideo(order, video1)
                val currentMemberReward = member.reward

                //when
                val refund = order.cancelVideoOrder(orderVideo1)

                //then
                Assertions.assertAll("취소되는 금액, 리워드 확인",
                    Executable {
                        assertThat(refund.refundAmount).isEqualTo(500)
                    },
                    Executable {
                        assertThat(refund.refundReward).isEqualTo(500)
                    }
                )

                Assertions.assertAll("orderVideo 취소 확인",
                    Executable {
                        assertThat(orderVideo1.orderStatus).isEqualTo(OrderStatus.CANCELED)
                    }
                )

                Assertions.assertAll("멤버 리워드 확인",
                    Executable {
                        assertThat(member.reward).isEqualTo(currentMemberReward + 500)
                    }
                )
            },
            dynamicTest("video2 를 취소한다.") {
                //given
                val orderVideo2 = getOrderVideo(order, video2)
                val currentMemberReward = member.reward

                //when
                val refund = order.cancelVideoOrder(orderVideo2)

                //then
                Assertions.assertAll("취소되는 금액, 리워드 확인",
                    Executable { assertThat(refund.refundAmount).isEqualTo(0) },
                    Executable { assertThat(refund.refundReward).isEqualTo(1000) }
                )

                Assertions.assertAll("멤버 리워드 확인",
                    Executable { assertThat(member.reward).isEqualTo(currentMemberReward + 1000) }
                )

                Assertions.assertAll("orderVideo 취소 확인",
                    Executable { assertThat(orderVideo2.orderStatus).isEqualTo(OrderStatus.CANCELED) }
                )

                Assertions.assertAll("order 취소 확인",
                    Executable { assertThat(order.orderStatus).isEqualTo(OrderStatus.CANCELED) },
                    Executable { assertThat(order.remainRefundAmount).isEqualTo(0) },
                    Executable { assertThat(order.remainRefundReward).isEqualTo(0) }
                )
            }
        )
    }

    @TestFactory
    @DisplayName("환불할 reward 가 부족한 상황일 때 결제 금액 혹은 리워드에서 차감하여 member 에 추가한다.")
    fun convertAmountToReward(): Collection<DynamicTest> {
        //given
        val member = Member.builder().reward(1000).build()
        val video1 = Video.builder().price(1000).build()
        val video2 = Video.builder().price(1000).build()

        val useReward = 1000

        val order = Order.createOrder(member, listOf(video1, video2), useReward)
        order.completeOrder(LocalDateTime.now(), "paymentKey")

        return listOf(
            dynamicTest("500원을 리워드에서 차감해서 member reward 로 추가한다.") {
                //given
                val currentMemberReward = member.reward
                val convertAmount = 500
                val orderRemainRefundReward = order.remainRefundReward

                //when
                order.convertAmountToReward(convertAmount)

                //then
                Assertions.assertAll("멤버 리워드 확인",
                    Executable { assertThat(member.reward).isEqualTo(currentMemberReward + convertAmount)})

                Assertions.assertAll("order 리워드 확인",
                    Executable { assertThat(order.remainRefundReward).isEqualTo(orderRemainRefundReward - convertAmount)})
            },
            dynamicTest("700원을 차감하면 500원은 리워드에서, 200원은 금액에서 차감한 후 member reward 로 추가한다.") {
                //given
                val currentMemberReward = member.reward
                val convertAmount = 700
                val orderRemainRefundReward = order.remainRefundReward
                val orderRemainRefundAmount = order.remainRefundAmount

                //when
                order.convertAmountToReward(convertAmount)

                //then
                Assertions.assertAll("멤버 리워드 확인",
                    Executable { assertThat(member.reward).isEqualTo(currentMemberReward + convertAmount)})

                Assertions.assertAll("order 리워드 확인",
                    Executable { assertThat(order.remainRefundReward).isEqualTo(orderRemainRefundReward - 500)},
                    Executable { assertThat(order.remainRefundAmount).isEqualTo(orderRemainRefundAmount - 200)})
            },
            dynamicTest("800원을 주문 금액에서 차감한 후 member reward 로 추가한다.") {

                //given
                val currentMemberReward = member.reward
                val convertAmount = 800
                val orderRemainRefundAmount = order.remainRefundAmount

                //when
                order.convertAmountToReward(convertAmount)

                //then
                Assertions.assertAll("멤버 리워드 확인",
                    Executable {
                        assertThat(member.reward).isEqualTo(currentMemberReward + convertAmount)
                    }
                )
                Assertions.assertAll("order 리워드 확인",
                    Executable {
                        assertThat(order.remainRefundAmount).isEqualTo(orderRemainRefundAmount - convertAmount)
                    }
                )
            },
            dynamicTest("주문 금액에서 차감 시 금액이 부족하면 RewardNotEnoughException 이 발생한다.") {

                //given
                val convertAmount = 500

                //when & then
                assertThatThrownBy {
                    order.convertAmountToReward(convertAmount)
                }
                    .isInstanceOf(RewardNotEnoughException::class.java)
            }
        )
    }

    private fun getOrderVideo(order: Order, video: Video): OrderVideo {
        return order.orderVideos.first { it.video == video }
    }
}
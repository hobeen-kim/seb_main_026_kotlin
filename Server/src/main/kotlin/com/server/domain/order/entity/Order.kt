package com.server.domain.order.entity

import com.server.domain.member.entity.Member
import com.server.domain.video.entity.Video
import com.server.global.exception.businessexception.orderexception.OrderAlreadyCanceledException
import com.server.global.exception.businessexception.orderexception.OrderNotValidException
import com.server.global.exception.businessexception.orderexception.PriceNotMatchException
import java.time.LocalDateTime
import java.util.*
import javax.persistence.*

@Entity(name = "orders")
class Order(
    @Id
    var orderId: String?= null,

    var paymentKey: String?= null,

    val totalPayAmount: Int,

    var remainRefundAmount: Int,

    val reward: Int,

    var remainRefundReward: Int,

    var completeDate: LocalDateTime?= null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var orderStatus: OrderStatus,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    val member: Member,

    @OneToMany(mappedBy = "order", cascade = [CascadeType.ALL])
    val orderVideos: MutableList<OrderVideo> = mutableListOf()
) {
    @PrePersist
    fun generatedUuid() {

        if (orderId == null) {
            orderId = UUID.randomUUID().toString()
        }
    }

    fun addOrderVideo(orderVideo: OrderVideo) {
        orderVideos.add(orderVideo)
        orderVideo.addOrder(this)
    }

    fun getVideos(): List<Video> {
        return orderVideos.map { it.video }
    }

    fun checkValidOrder(amount: Int) {
        checkOrdered()
        checkAmount(amount)
    }

    private fun checkOrdered() {
        if (orderStatus != OrderStatus.ORDERED) {
            throw OrderNotValidException()
        }
    }

    private fun checkAmount(amount: Int) {
        if (this.totalPayAmount != amount) {
            throw PriceNotMatchException()
        }
    }

    fun completeOrder(completeDate: LocalDateTime, paymentKey: String) {
        this.completeDate = completeDate
        this.paymentKey = paymentKey
        this.orderStatus = OrderStatus.COMPLETED
        this.orderVideos.forEach { it.complete() }
        this.remainRefundAmount = totalPayAmount
        this.remainRefundReward = reward
    }

    fun cancelAllOrder(): Refund {
        this.orderVideos.forEach { it.cancel() }

        if (isComplete()) {
            this.member.addReward(this.remainRefundReward)
        }

        cancel()

        val refund = Refund(remainRefundAmount, remainRefundReward)

        this.remainRefundAmount = 0
        this.remainRefundReward = 0

        return refund
    }

    fun isComplete(): Boolean {
        return orderStatus == OrderStatus.COMPLETED
    }

    private fun cancel() {
        this.orderStatus = OrderStatus.CANCELED
    }

    fun checkAlreadyCanceled() {
        if (orderStatus == OrderStatus.CANCELED) {
            throw OrderAlreadyCanceledException()
        }
    }

    fun cancelVideoOrder(orderVideo: OrderVideo): Refund {
        orderVideo.cancel()

        if (allVideoIsCanceled()) return cancelAllOrder()

        val refundAmount = calculateRefundAmount(orderVideo)
        val refundReward = calculateRefundReward(orderVideo.video - refundAmount)

        this.member.addReward(refundReward)

        return Refund(refundAmount, refundReward)
    }

    private fun allVideoIsCanceled(): Boolean {
        return orderVideos.all { it.orderStatus == OrderStatus.CANCELED }
    }

    private fun calculateRefundAmount(orderVideo: OrderVideo): Int {
        var refundAmount = orderVideo.price

        if (this.remainRefundAmount < refundAmount) {
            refundAmount = this.remainRefundAmount
            this.remainRefundAmount = 0
            return refundAmount
        }

        this.remainRefundAmount -= refundAmount
        return refundAmount
    }

    private fun calculateRefundReward(refundReward: Int): Int {

        var totalRefundReward: Int

        if (this.remainRefundReward < refundReward) {
            totalRefundReward = this.remainRefundReward
            this.remainRefundReward = 0
            return totalRefundReward
        }
        this.remainRefundReward -= refundReward
        return refundReward
    }

    fun isExpired() = this.completeDate!!.plusDays(14).isBefore(LocalDateTime.now())

    companion object {
        fun createOrder(
            member: Member,
            videos: List<Video>,
            reward: Int
        ): Order {
            val totalPayAmount = videos.sumOf { it.price } - reward

            if (totalPayAmount < 0) {
                throw IllegalArgumentException("결제 금액이 0보다 작습니다.")
            }

            member.checkReward(reward)

            val order = Order(
                member = member,
                totalPayAmount = totalPayAmount,
                reward = reward,
                remainRefundAmount = 0,
                remainRefundReward = 0,
                orderStatus = OrderStatus.ORDERED
            )

            videos.forEach {
                order.addOrderVideo(OrderVideo.createOrderVideo(order, it, it.price))
            }

            return order
        }

        class Refund(
            val refundAmount: Int,
            val refundReward: Int
        )

    }
}


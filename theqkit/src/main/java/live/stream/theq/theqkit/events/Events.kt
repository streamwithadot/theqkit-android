package live.stream.theq.theqkit.events

import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import live.stream.theq.theqkit.TheQKit

internal object Events {

  private val eventPubSub = PublishSubject.create<Event>()

  fun getEventStream(isPartnerImplementation: Boolean): Observable<Event> {
    return eventPubSub.filter { it.sdkVisible || !isPartnerImplementation }
  }

  fun publish(event: Event) {
    TheQKit.getInstance().config.partnerCode
    eventPubSub.onNext(event)
  }

}

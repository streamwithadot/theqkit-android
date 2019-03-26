package live.stream.theq.theqkit.events

import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

internal object Events {

  private val eventPubSub = PublishSubject.create<Event>()

  val eventStream: Observable<Event>
    get() = eventPubSub

  fun publish(event: Event) = eventPubSub.onNext(event)

}
package play.modules.spring.scoping;

import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.joda.time.DateTime;

public class ScopeUnit {

	private static final Logger log = Logger.getLogger(ScopeUnit.class);

	private static final ScheduledExecutorService SCOPE_DESTRUCTION_THREAD = Executors.newScheduledThreadPool(1);
	private static final long NO_TIMEOUT = 0;

	private final DateTime creationTime;
	private final long destructionTimoutMillis;
	private final String conversationId;
	private final Map<String, Object> beans = new WeakHashMap<String, Object>();
	private final Map<String, Runnable> destructionCallbacks = new HashMap<String, Runnable>();
	private ScheduledFuture<?> futureDestruction;

	public ScopeUnit(final String conversationId, final long destructionTimoutMillis) {
		log.debug("Creating sope unit for conversation " + conversationId);
		this.conversationId = conversationId;
		this.creationTime = new DateTime();
		this.destructionTimoutMillis = destructionTimoutMillis;

		if (this.destructionTimoutMillis != NO_TIMEOUT) {
			rescheduleDestruction();
		}
	}

	public ScopeUnit(final String conversationId) {
		this(conversationId, NO_TIMEOUT);
	}

	public Object getBean(final String beanName) {
		if (this.destructionTimoutMillis != NO_TIMEOUT) {
			rescheduleDestruction();
		}
		return this.beans.get(beanName);
	}

	public void addBean(final String beanName, final Object bean) {
		if (this.destructionTimoutMillis != NO_TIMEOUT) {
			rescheduleDestruction();
		}

		this.beans.put(beanName, bean);
	}

	public void registerDestructionCallback(final String beanName, final Runnable callback) {
		this.destructionCallbacks.put(beanName, callback);
	}

	public void destroy() {
		log.debug("Destroying unit " + this.conversationId);
		for (final Runnable callback : this.destructionCallbacks.values()) {
			callback.run();
		}
	}

	public Object removeBean(final String beanName) {
		if (this.destructionTimoutMillis != NO_TIMEOUT) {
			rescheduleDestruction();
		}

		return this.beans.remove(beanName);
	}

	private void rescheduleDestruction() {

		if (this.futureDestruction != null) {
			this.futureDestruction.cancel(true);
		}

		this.futureDestruction = SCOPE_DESTRUCTION_THREAD.schedule(new Runnable() {

			@Override
			public void run() {
				log.debug("Destroying this scope unit");
				destroy();
				beans.clear();
			}
		}, this.destructionTimoutMillis, TimeUnit.MILLISECONDS);

		log.debug("Destruction scheuled for " + this.destructionTimoutMillis + "ms");
	}

}

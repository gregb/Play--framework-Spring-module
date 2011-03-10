package play.modules.spring.scoping;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ObjectFactory;

public class AbstractScopeTest {

	@Ignore
	private static class TestScope extends AbstractScope {
		private String conversationId;

		@Override
		public String getConversationId() {
			return conversationId;
		}

		public void setConversationId(final String conversationId) {
			this.conversationId = conversationId;
		}

		@Override
		public Object resolveContextualObject(final String arg0) {
			return null;
		}
	}

	public static class CountingIntFactory implements ObjectFactory<Integer> {
		Integer i = 0;

		@Override
		public Integer getObject() throws BeansException {
			i = new Integer(i + 1);
			return i;
		}

	}

	@Test
	public void testGetWithFactoryInteraction() {
		final CountingIntFactory factory = new CountingIntFactory();

		final TestScope session = new TestScope();
		session.setConversationId("session1");

		// does not exist, pulls from factory
		final Object get1bean1 = session.get("bean1", factory);
		Assert.assertEquals(1, get1bean1);

		// already exists, pulls same bean
		final Object get2bean1 = session.get("bean1", factory);
		Assert.assertEquals(1, get2bean1);
		Assert.assertSame(get1bean1, get2bean1);

		// does not exist, pulls next from factory
		final Object get3bean2 = session.get("bean2", factory);
		Assert.assertEquals(2, get3bean2);
		Assert.assertNotSame(get3bean2, get1bean1);

		// already exists, pulls same bean
		final Object get4bean2 = session.get("bean2", factory);
		Assert.assertEquals(2, get4bean2);
		Assert.assertSame(get3bean2, get4bean2);

		// remove bean 1
		final Object remove1 = session.remove("bean1");
		Assert.assertSame(get1bean1, remove1);

		// should not exist, pulls from factory
		final Object get5bean1 = session.get("bean1", factory);
		Assert.assertEquals(3, get5bean1);
		Assert.assertNotSame(get1bean1, get5bean1);
	}

	@Test
	public void testContextSwitch() {
		final TestScope session = new TestScope();
		final CountingIntFactory factory = new CountingIntFactory();

		// simulate current context being session 1
		session.setConversationId("session1");

		// does not exist, pulls from factory
		final Object session1bean1get1 = session.get("bean1", factory);
		Assert.assertEquals(1, session1bean1get1);

		// switch context
		session.setConversationId("session2");

		// does not exist in this session yet, pulls from factory
		final Object session2bean1get1 = session.get("bean1", factory);
		Assert.assertEquals(2, session2bean1get1);
		Assert.assertNotSame(session1bean1get1, session2bean1get1);

		// switch back
		session.setConversationId("session1");

		// should get the same as before
		final Object session1bean1get2 = session.get("bean1", factory);
		Assert.assertEquals(1, session1bean1get2);
		Assert.assertSame(session1bean1get1, session1bean1get1);
	}

}

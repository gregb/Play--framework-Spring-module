package play.modules.spring.scoping;

import junit.framework.Assert;

import org.junit.Test;

public class ScopeUnitTest {

	class Flagger {
		boolean flag = false;
	}

	@Test
	public void testAddGetRemove() {

		Long bean = 123456789L;
		String bean2 = "qwerty";

		ScopeUnit unit = new ScopeUnit("1");
		unit.addBean("a", bean);
		unit.addBean("b", bean2);

		Assert.assertSame(bean, unit.getBean("a"));
		Assert.assertSame(bean2, unit.getBean("b"));

		unit.removeBean("a");

		Assert.assertNull(unit.getBean("a"));
		Assert.assertSame(bean2, unit.getBean("b"));

		unit.removeBean("b");

		Assert.assertNull(unit.getBean("a"));
		Assert.assertNull(unit.getBean("b"));
	}

	@Test
	public void testTimeout() throws Exception {

		Long bean = 123456789L;
		ScopeUnit unit = new ScopeUnit("1", 5 * 1000);
		final Flagger x = new Flagger();

		unit.addBean("a", bean);
		unit.registerDestructionCallback("a", new Runnable() {
			@Override
			public void run() {
				x.flag = true;
			}
		});

		Assert.assertFalse(x.flag);
		Assert.assertNotNull(unit.getBean("a"));
		Assert.assertSame(bean, unit.getBean("a"));
		Assert.assertFalse(x.flag);

		Thread.sleep(6 * 1000);

		Assert.assertTrue(x.flag);
		Assert.assertNull(unit.getBean("a"));
	}

}

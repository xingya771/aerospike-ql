package com.spikeify.aerospikeql.execute;

import com.spikeify.Spikeify;
import com.spikeify.aerospikeql.AerospikeQlService;
import com.spikeify.aerospikeql.Executor;
import com.spikeify.aerospikeql.TestAerospike;
import com.spikeify.aerospikeql.entities.Entity1;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.assertEquals;

public class ProfileTests {

	private Spikeify sfy;
	private AerospikeQlService aerospikeQlService;

	@Before
	public void setUp(){
		TestAerospike testAerospike = new TestAerospike();
		sfy = testAerospike.getSfy();
		aerospikeQlService = new AerospikeQlService(sfy);
		sfy.truncateNamespace(TestAerospike.getDefaultNamespace());
	}

	@After
	public void tearDown() {
		sfy.truncateNamespace(TestAerospike.getDefaultNamespace());
	}

	private void createSet(int numRecords) {
		Entity1 entity;
		for (int i = 1; i < numRecords + 1; i++) {
			entity = new Entity1();
			entity.key = String.valueOf(i);
			entity.value = i;
			entity.value2 = i + 1;
			entity.cluster = i % 4;
			sfy.create(entity).now();
		}

		int i = numRecords + 2;
		entity = new Entity1();
		entity.key = String.valueOf(i);
		entity.value = null;
		entity.value2 = i + 1;
		entity.cluster = null;
		sfy.create(entity).now();
	}

	@Test
	public void testProfileWithCount() throws Exception {
		createSet(100);
		String query = "select count(1) as counter1, count(*) as counter2  from " + TestAerospike.getDefaultNamespace() + ".Entity1";

		Executor<Map<String, Object>> executor = aerospikeQlService.execAdhoc(query);
		executor.now();

		Profile profile = executor.getProfile();
		assertEquals(0L, profile.getColumnsQueried());
		assertEquals(1L, profile.getRowsRetrieved());
		assertEquals(101L, profile.getRowsQueried());

	}

	@Test
	public void testProfileWithAggs() throws Exception {
		createSet(100);
		String query = "select sum(value) as sumValue, " +
						"avg(value) as avgValue, " +
						"min(value) as minValue," +
						"max(value) as maxValue," +
						"count(*) as counter " +
						"from " + TestAerospike.getDefaultNamespace() + ".Entity1";

		Executor<Map<String, Object>> executor = aerospikeQlService.execAdhoc(query);
		executor.now();

		Profile profile = executor.getProfile();
		assertEquals(1L, profile.getColumnsQueried());
		assertEquals(1L, profile.getRowsRetrieved());
		assertEquals(101L, profile.getRowsQueried());
	}

	@Test
	public void testProfileWithHaving1() throws Exception {
		createSet(100);
		String query = "select cluster," +
						"sum(value) as sumValue, " +
						"avg(value) as avgValue, " +
						"min(value) as minValue," +
						"max(value) as maxValue," +
						"count(*) as counter " +
						"from " + TestAerospike.getDefaultNamespace() + ".Entity1 " +
						"group by cluster " +
						"having avgValue > 50 and maxValue = 99";

		Executor<Map<String, Object>> executor = aerospikeQlService.execAdhoc(query);
		executor.now();

		Profile profile = executor.getProfile();
		assertEquals(2L, profile.getColumnsQueried());
		assertEquals(1L, profile.getRowsRetrieved());
		assertEquals(101L, profile.getRowsQueried());
	}

}

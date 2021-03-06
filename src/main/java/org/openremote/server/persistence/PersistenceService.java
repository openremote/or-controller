/*
 * Copyright 2015, OpenRemote Inc.
 *
 * See the CONTRIBUTORS.txt file in the distribution for a
 * full listing of individual contributors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package org.openremote.server.persistence;

import org.apache.camel.StaticService;
import org.openremote.server.persistence.flow.FlowDAO;
import org.openremote.server.persistence.flow.FlowDAOImpl;
import org.openremote.server.persistence.inventory.ClientPresetDAO;
import org.openremote.server.persistence.inventory.ClientPresetDAOImpl;
import org.openremote.server.persistence.inventory.DeviceDAO;
import org.openremote.server.persistence.inventory.DeviceDAOImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.transaction.UserTransaction;
import java.util.HashMap;
import java.util.Map;

public class PersistenceService implements StaticService {

    public interface Work<T> {
        T doWork(PersistenceService persistenceService, EntityManager em);
    }

    private static final Logger LOG = LoggerFactory.getLogger(PersistenceService.class);

    public static final String PERSISTENCE_UNIT_NAME = "ControllerPU";

    final protected TransactionManagerService transactionManagerService;
    final protected DatabaseProduct databaseProduct;
    final protected Map<String, String> properties = new HashMap<>();

    protected EntityManagerFactory entityManagerFactory;

    public PersistenceService(TransactionManagerService transactionManagerService, DatabaseProduct databaseProduct) {
        this.transactionManagerService = transactionManagerService;
        this.databaseProduct = databaseProduct;
    }

    @Override
    public void start() throws Exception {
        LOG.info("Configuration JPA service for database product: " + databaseProduct);

        properties.put("hibernate.dialect", databaseProduct.getHibernateDialect());

        entityManagerFactory =
            Persistence.createEntityManagerFactory(PERSISTENCE_UNIT_NAME, properties);
    }

    @Override
    public void stop() throws Exception {
        entityManagerFactory.close();
    }

    // TODO If Camel transacted() wouldn't be crap, we could use it instead of this...
    // TODO https://issues.apache.org/jira/browse/CAMEL-9254
    public <T> T transactional(Work<T> work) {
        UserTransaction tx = transactionManagerService.getUserTransaction();
        try {
            tx.begin();
            try {
                EntityManager em = createEntityManager();
                T result = work.doWork(this, em);
                tx.commit();
                em.close();
                return result;
            } finally {
                transactionManagerService.rollback();
            }
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    @SuppressWarnings("unchecked")
    public <D extends GenericDAO> D getDAO(EntityManager em, Class<D> type) {
        if (type == FlowDAO.class) {
            return (D) new FlowDAOImpl(em);
        } else if (type == DeviceDAO.class) {
            return (D) new DeviceDAOImpl(em);
        } else if (type == ClientPresetDAO.class) {
            return (D) new ClientPresetDAOImpl(em);
        }
        throw new UnsupportedOperationException("Unsupported DAO type: " + type);
    }

    public EntityManagerFactory getEntityManagerFactory() {
        return entityManagerFactory;
    }

    public EntityManager createEntityManager() {
        return getEntityManagerFactory().createEntityManager();
    }

    public void createSchema() {
        generateSchema("create");
    }

    public void dropSchema() {
        generateSchema("drop");
    }

    public void generateSchema(String action) {
        // Take exiting EMF properties, override the schema generation setting on a copy
        Map<String, String> createSchemaProperties = new HashMap<>(properties);
        createSchemaProperties.put(
            "javax.persistence.schema-generation.database.action",
            action
        );
        Persistence.generateSchema(PERSISTENCE_UNIT_NAME, createSchemaProperties);
    }

}

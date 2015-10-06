package org.openremote.server.inventory.discovery;

import org.apache.camel.Header;
import org.apache.camel.StaticService;
import org.openremote.shared.inventory.Adapter;
import org.openremote.devicediscovery.domain.DiscoveredDeviceDTO;

import java.util.*;

public class InboxService implements StaticService {

    final protected AdapterDiscoveryService adapterDiscoveryService;
    final protected Set<Adapter> adapters = new LinkedHashSet<>();
    final protected List<DiscoveredDeviceDTO> discoveredDevices = new ArrayList<>(); // TODO WE DON'T HAVE ID AND GET DUPLICATES!

    public InboxService(AdapterDiscoveryService adapterDiscoveryService) {
        this.adapterDiscoveryService = adapterDiscoveryService;
    }

    @Override
    public void start() throws Exception {
    }

    @Override
    public void stop() throws Exception {

    }

    public void addAdapter(Adapter adapter) throws Exception {
        synchronized (adapters) {
            if (adapter == null) {
                throw new IllegalArgumentException("Adapter is null");
            }
            adapterDiscoveryService.updateDiscoveryRoute(adapter, true);
            adapters.add(adapter);
        }
    }

    public void removeAdapter(@Header("adapterId") String id) throws Exception {
        synchronized (adapters) {
            Iterator<Adapter> it = adapters.iterator();
            while (it.hasNext()) {
                Adapter adapter = it.next();
                if (adapter.getId().equals(id)) {
                    adapterDiscoveryService.updateDiscoveryRoute(adapter, false);
                    it.remove();
                }
            }
        }
    }

    public Adapter[] getAdapters() {
        synchronized (adapters) {
            return adapters.toArray(new Adapter[adapters.size()]);
        }
    }

    public void triggerDiscovery(Adapter adapter) throws Exception {
        synchronized (adapters) {
            for (Adapter a: adapters) {
                if (adapter == null || a.equals(adapter))
                    adapterDiscoveryService.triggerDiscovery(a);
            }
        }
    }

    public void triggerDiscovery() throws Exception {
        triggerDiscovery(null);
    }

    public DiscoveredDeviceDTO[] getDiscoveredDevices() {
        synchronized (discoveredDevices) {
            return discoveredDevices.toArray(new DiscoveredDeviceDTO[discoveredDevices.size()]);
        }
    }

    public void addDiscoveredDevices(List<DiscoveredDeviceDTO> devices) {
        synchronized (discoveredDevices) {
            discoveredDevices.addAll(devices);
        }
    }
}
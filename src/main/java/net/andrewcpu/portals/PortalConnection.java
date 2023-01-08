package net.andrewcpu.portals;

public class PortalConnection {
    private Portal portal1;
    private Portal portal2;

    public PortalConnection(Portal portal1, Portal portal2) {
        this.portal1 = portal1;
        this.portal2 = portal2;
    }

    public Portal getPortal1() {
        return portal1;
    }

    public void setPortal1(Portal portal1) {
        this.portal1 = portal1;
    }

    public Portal getPortal2() {
        return portal2;
    }

    public void setPortal2(Portal portal2) {
        this.portal2 = portal2;
    }
}

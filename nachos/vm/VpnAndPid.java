package nachos.vm;

import java.util.Objects;

public class VpnAndPid {
    private int vpn;
    private int pid;

    public VpnAndPid(int vpn, int pid) {
        this.vpn = vpn;
        this.pid = pid;
    }

    public int getVpn() {
        return vpn;
    }

    public void setVpn(int vpn) {
        this.vpn = vpn;
    }

    public int getPid() {
        return pid;
    }

    public void setPid(int pid) {
        this.pid = pid;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VpnAndPid vpnAndPid = (VpnAndPid) o;
        return vpn == vpnAndPid.vpn && pid == vpnAndPid.pid;
    }

    @Override
    public int hashCode() {

        int hash=(pid +Integer.toString(vpn)).hashCode();

        return hash;


//        return Objects.hash(vpn, pid);
    }
}

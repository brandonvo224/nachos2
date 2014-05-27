package nachos.vm;

import java.util.*;
import nachos.machine.*;
import nachos.userprog.*;
import nachos.threads.*;
import nachos.threads.Lock;

/* Now in order to translate a virtual address, 
 * the process ID and virtual page number are hashed to get
 * an entry in the hash anchor table.*/

public class TEKey{
	int vpn;
	int pid;

	@Override
	public int hashCode(){
		return new String(vpn + "-" + pid).hashCode();
	}
	@Override
	public boolean equals(Object o){
		if(obj instanceOf TEKey){
			TEKey obj = (TEKey) o;
			return (obj.vpn == this.vpn && obj.pid == this.pid);
		}
		return false;	
	}
}


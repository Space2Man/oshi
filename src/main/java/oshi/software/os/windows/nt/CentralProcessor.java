/**
 * Oshi (https://github.com/dblock/oshi)
 * 
 * Copyright (c) 2010 - 2015 The Oshi Project Team
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 * dblock[at]dblock[dot]org
 * alessandro[at]perucchi[dot]org
 * widdis[at]gmail[dot]com
 * https://github.com/dblock/oshi/graphs/contributors
 */
package oshi.software.os.windows.nt;

import java.lang.management.ManagementFactory;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import oshi.hardware.Processor;
import oshi.util.ParseUtil;

import com.sun.jna.LastErrorException;
import com.sun.jna.Native;
import com.sun.jna.platform.win32.WinBase;
import com.sun.management.OperatingSystemMXBean;

/**
 * A CPU as defined in Windows registry.
 * 
 * @author dblock[at]dblock[dot]org
 * @author alessio.fachechi[at]gmail[dot]com
 */
@SuppressWarnings("restriction")
public class CentralProcessor implements Processor {
	private static final OperatingSystemMXBean OS_MXBEAN;
	static {
		OS_MXBEAN = (com.sun.management.OperatingSystemMXBean) ManagementFactory
				.getOperatingSystemMXBean();
		// Initialize CPU usage
		OS_MXBEAN.getSystemCpuLoad();
	}

	private String _vendor;
	private String _name;
	private String _identifier;
	private Long _freq;

	public CentralProcessor() {

	}

	/**
	 * Vendor identifier, eg. GenuineIntel.
	 * 
	 * @return Processor vendor.
	 */
	@Override
	public String getVendor() {
		return this._vendor;
	}

	/**
	 * Set processor vendor.
	 * 
	 * @param vendor
	 *			Vendor.
	 */
	@Override
	public void setVendor(String vendor) {
		this._vendor = vendor;
	}

	/**
	 * Name, eg. Intel(R) Core(TM)2 Duo CPU T7300 @ 2.00GHz
	 * 
	 * @return Processor name.
	 */
	@Override
	public String getName() {
		return this._name;
	}

	/**
	 * Set processor name.
	 * 
	 * @param name
	 *			Name.
	 */
	@Override
	public void setName(String name) {
		this._name = name;
	}

	/**
	 * Vendor frequency (in Hz), eg. for processor named Intel(R) Core(TM)2 Duo
	 * CPU T7300 @ 2.00GHz the vendor frequency is 2000000000.
	 * 
	 * @return Processor frequency or -1 if unknown.
	 */
	@Override
	public long getVendorFreq() {
		if (this._freq == null) {
			Pattern pattern = Pattern.compile("@ (.*)$");
			Matcher matcher = pattern.matcher(getName());

			if (matcher.find()) {
				String unit = matcher.group(1);
				this._freq = Long.valueOf(ParseUtil.parseHertz(unit));
			} else {
				this._freq = Long.valueOf(-1L);
			}
		}

		return this._freq.longValue();
	}

	/**
	 * Set vendor frequency.
	 * 
	 * @param freq
	 *			Frequency.
	 */
	@Override
	public void setVendorFreq(long freq) {
		this._freq = Long.valueOf(freq);
	}

	/**
	 * Identifier, eg. x86 Family 6 Model 15 Stepping 10.
	 * 
	 * @return Processor identifier.
	 */
	@Override
	public String getIdentifier() {
		return this._identifier;
	}

	/**
	 * Set processor identifier.
	 * 
	 * @param identifier
	 *			Identifier.
	 */
	@Override
	public void setIdentifier(String identifier) {
		this._identifier = identifier;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isCpu64bit() {
		throw new UnsupportedOperationException();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setCpu64(boolean cpu64) {
		throw new UnsupportedOperationException();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getStepping() {
		throw new UnsupportedOperationException();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setStepping(String _stepping) {
		throw new UnsupportedOperationException();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getModel() {
		throw new UnsupportedOperationException();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setModel(String _model) {
		throw new UnsupportedOperationException();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getFamily() {
		throw new UnsupportedOperationException();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setFamily(String _family) {
		throw new UnsupportedOperationException();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	@Deprecated
	public float getLoad() {
		long[] prevTicks = getCpuLoadTicks();
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			// Awake, O sleeper
		}
		long[] ticks = getCpuLoadTicks();
		long total = 0;
		for (int i = 0; i < ticks.length; i++) {
			total += (ticks[i] - prevTicks[i]);
		}
		long idle = ticks[ticks.length - 1] - prevTicks[ticks.length - 1];
		if (total > 0 && idle >= 0) {
			return 100f * (total - idle) / total;
		}
		return 0f;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public long[] getCpuLoadTicks() {
		WinBase.FILETIME lpIdleTime = new WinBase.FILETIME();
		WinBase.FILETIME lpKernelTime = new WinBase.FILETIME();
		WinBase.FILETIME lpUserTime = new WinBase.FILETIME();
		if (0 == Kernel32.INSTANCE.GetSystemTimes(lpIdleTime, lpKernelTime,
				lpUserTime))
			throw new LastErrorException("Error code: " + Native.getLastError());
		// Array order is user,nice,kernel,idle
		long[] ticks = new long[4];
		ticks[0] = lpUserTime.toLong() + Kernel32.WIN32_TIME_OFFSET;
		ticks[1] = 0L; // Windows is not 'nice'
		ticks[2] = lpKernelTime.toLong() - lpIdleTime.toLong();
		ticks[3] = lpIdleTime.toLong() + Kernel32.WIN32_TIME_OFFSET;
		return ticks;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public double getSystemCPULoad() {
		return OS_MXBEAN.getSystemCpuLoad();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public double getSystemLoadAverage() {
		return OS_MXBEAN.getSystemLoadAverage();
	}

	@Override
	public String toString() {
		return getName();
	}
}
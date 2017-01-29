/**
 * Copyright (C) 2013 Aurélien Chabot <aurelien@chabot.fr>
 * 
 * This file is part of DroidUPNP.
 * 
 * DroidUPNP is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * DroidUPNP is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with DroidUPNP.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.droidupnp.model.upnp;

public interface IUpnpDevice {

	String getDisplayString();

	String getFriendlyName();

	String getExtendedInformation();

	String getManufacturer();

	String getManufacturerURL();

	String getModelName();

	String getModelDesc();

	String getModelNumber();

	String getModelURL();

	String getXMLURL();

	String getPresentationURL();

	String getSerialNumber();

	String getUDN();

	boolean equals(IUpnpDevice otherDevice);

	String getUID();

	boolean asService(String service);

	void printService();

	boolean isFullyHydrated();

	@Override
	String toString();
}

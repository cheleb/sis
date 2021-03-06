/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sis.metadata.iso.citation;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collection;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import org.opengis.util.InternationalString;
import org.opengis.metadata.citation.Address;
import org.opengis.metadata.citation.Contact;
import org.opengis.metadata.citation.Telephone;
import org.opengis.metadata.citation.OnlineResource;
import org.opengis.metadata.citation.TelephoneType;
import org.apache.sis.metadata.iso.ISOMetadata;
import org.apache.sis.util.resources.Messages;
import org.apache.sis.internal.jaxb.Context;
import org.apache.sis.internal.metadata.LegacyPropertyAdapter;


/**
 * Information required to enable contact with the responsible person and/or organization.
 *
 * <p><b>Limitations:</b></p>
 * <ul>
 *   <li>Instances of this class are not synchronized for multi-threading.
 *       Synchronization, if needed, is caller's responsibility.</li>
 *   <li>Serialized objects of this class are not guaranteed to be compatible with future Apache SIS releases.
 *       Serialization support is appropriate for short term storage or RMI between applications running the
 *       same version of Apache SIS. For long term storage, use {@link org.apache.sis.xml.XML} instead.</li>
 * </ul>
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @author  Touraïvane (IRD)
 * @author  Cédric Briançon (Geomatys)
 * @author  Rémi Maréchal (Geomatys)
 * @since   0.3
 * @version 0.5
 * @module
 */
@XmlType(name = "CI_Contact_Type", propOrder = {
    "phone",
    "address",
    "onlineResource",
    "hoursOfService",
    "contactInstructions"
})
@XmlRootElement(name = "CI_Contact")
public class DefaultContact extends ISOMetadata implements Contact {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = -969735574940462381L;

    /**
     * Telephone numbers at which the organization or individual may be contacted.
     */
    private Collection<Telephone> phones;

    /**
     * Physical and email addresses at which the organization or individual may be contacted.
     */
    private Collection<Address> addresses;

    /**
     * On-line information that can be used to contact the individual or organization.
     */
    private Collection<OnlineResource> onlineResources;

    /**
     * Time period (including time zone) when individuals can contact the organization or individual.
     */
    private Collection<InternationalString> hoursOfService;

    /**
     * Supplemental instructions on how or when to contact the individual or organization.
     */
    private InternationalString contactInstructions;

    /**
     * Type of the contact.
     */
    private InternationalString contactType;

    /**
     * Constructs an initially empty contact.
     */
    public DefaultContact() {
    }

    /**
     * Constructs a contact initialized to the specified online resource.
     *
     * @param resource The on-line information that can be used to contact the individual or
     *        organization, or {@code null} if none.
     */
    public DefaultContact(final OnlineResource resource) {
        this.onlineResources = singleton(resource, OnlineResource.class);
    }

    /**
     * Constructs a new instance initialized with the values from the specified metadata object.
     * This is a <cite>shallow</cite> copy constructor, since the other metadata contained in the
     * given object are not recursively copied.
     *
     * @param object The metadata to copy values from, or {@code null} if none.
     *
     * @see #castOrCopy(Contact)
     */
    public DefaultContact(final Contact object) {
        super(object);
        if (object != null) {
            phones              = copyCollection(object.getPhones(), Telephone.class);
            addresses           = copyCollection(object.getAddresses(), Address.class);
            onlineResources     = copyCollection(object.getOnlineResources(), OnlineResource.class);
            hoursOfService      = copyCollection(object.getHoursOfService(), InternationalString.class);
            contactInstructions = object.getContactInstructions();
            contactType         = object.getContactType();
        }
    }

    /**
     * Returns a SIS metadata implementation with the values of the given arbitrary implementation.
     * This method performs the first applicable action in the following choices:
     *
     * <ul>
     *   <li>If the given object is {@code null}, then this method returns {@code null}.</li>
     *   <li>Otherwise if the given object is already an instance of
     *       {@code DefaultContact}, then it is returned unchanged.</li>
     *   <li>Otherwise a new {@code DefaultContact} instance is created using the
     *       {@linkplain #DefaultContact(Contact) copy constructor}
     *       and returned. Note that this is a <cite>shallow</cite> copy operation, since the other
     *       metadata contained in the given object are not recursively copied.</li>
     * </ul>
     *
     * @param  object The object to get as a SIS implementation, or {@code null} if none.
     * @return A SIS implementation containing the values of the given object (may be the
     *         given object itself), or {@code null} if the argument was null.
     */
    public static DefaultContact castOrCopy(final Contact object) {
        if (object == null || object instanceof DefaultContact) {
            return (DefaultContact) object;
        }
        return new DefaultContact(object);
    }

    /**
     * Returns telephone numbers at which the organization or individual may be contacted.
     *
     * @return Telephone numbers at which the organization or individual may be contacted.
     *
     * @since 0.5
     */
    @Override
    public Collection<Telephone> getPhones() {
        return phones = nonNullCollection(phones, Telephone.class);
    }

    /**
     * Sets telephone numbers at which the organization or individual may be contacted.
     *
     * @param newValues The new telephones.
     *
     * @since 0.5
     */
    public void setPhones(Collection<? extends Telephone> newValues) {
        phones = writeCollection(newValues, phones, Telephone.class);
        /*
         * Code below this point will be deleted after we removed the deprecated methods in DefaultTelephone.
         * This code notifies all DefaultTelephone instances about the the list of phones in order to allow
         * the deprecated Telephone.getVoices() and Telephone.getFacsimiles() methods to fetches information
         * from the phones list.
         */
        if (phones != null) {
            boolean modified = false;
            final Telephone[] p = phones.toArray(new Telephone[newValues.size()]);
            for (int i=0; i<p.length; i++) {
                final Telephone phone = p[i];
                if (phone instanceof DefaultTelephone) {
                    p[i] = ((DefaultTelephone) phone).setOwner(phones);
                    modified |= (p[i] != phone);
                }
            }
            if (modified) {
                phones.clear();
                phones.addAll(Arrays.asList(p));
            }
        }
    }

    /**
     * Returns telephone numbers at which the organization or individual may be contacted.
     * This method returns the first telephone number associated to {@link TelephoneType#VOICE}
     * or {@link TelephoneType#FACSIMILE FACSIMILE}.
     *
     * @return Telephone numbers at which the organization or individual may be contacted, or {@code null}.
     *
     * @deprecated As of ISO 19115:2014, replaced by {@link #getPhones()}.
     */
    @Override
    @Deprecated
    @XmlElement(name = "phone")
    public Telephone getPhone() {
        Telephone phone = null;
        final Collection<Telephone> phones = getPhones();
        if (phones != null) { // May be null on marshalling.
            TelephoneType ignored = null;
            for (final Telephone c : phones) {
                final TelephoneType type = c.getNumberType();
                if (TelephoneType.VOICE.equals(type) || TelephoneType.FACSIMILE.equals(type)) {
                    if (phone == null) {
                        phone = c;
                    }
                } else if (ignored == null) {
                    ignored = type;
                }
            }
            if (ignored != null) {
                Context.warningOccured(Context.current(), LOGGER, DefaultContact.class, "getPhone",
                        Messages.class, Messages.Keys.IgnoredPropertyAssociatedTo_1, ignored.toString());
            }
        }
        return phone;
    }

    /**
     * Sets telephone numbers at which the organization or individual may be contacted.
     * This method delegates to {@link #setPhones(Collection)}.
     *
     * @param newValue The new telephone, or {@code null} if none.
     *
     * @deprecated As of ISO 19115:2014, replaced by {@link #setPhones(Collection)}.
     */
    @Deprecated
    public void setPhone(Telephone newValue) {
        Collection<Telephone> newValues = null;
        if (newValue != null) {
            if (newValue instanceof DefaultTelephone) {
                newValues = ((DefaultTelephone) newValue).getOwner();
            } else {
                newValues = new ArrayList<>(4);
                for (String number : newValue.getVoices()) {
                    newValues.add(new DefaultTelephone(number, TelephoneType.VOICE));
                }
                for (String number : newValue.getFacsimiles()) {
                    newValues.add(new DefaultTelephone(number, TelephoneType.FACSIMILE));
                }
            }
        }
        setPhones(newValues);
    }

    /**
     * Returns the physical and email addresses at which the organization or individual may be contacted.
     *
     * @return Physical and email addresses at which the organization or individual may be contacted, or {@code null}.
     *
     * @since 0.5
     */
    @Override
    public Collection<Address> getAddresses() {
        return addresses = nonNullCollection(addresses, Address.class);
    }

    /**
     * Sets the physical and email addresses at which the organization or individual may be contacted.
     *
     * @param newValues The new addresses.
     *
     * @since 0.5
     */
    public void setAddresses(final Collection<? extends Address> newValues) {
        addresses = writeCollection(newValues, addresses, Address.class);
    }

    /**
     * Returns the physical and email address at which the organization or individual may be contacted.
     * This method returns the first {@link #getAddresses() adress} element, or null if none.
     *
     * @return Physical and email address at which the organization or individual may be contacted, or {@code null}.
     *
     * @deprecated As of ISO 19115:2014, replaced by {@link #getAddresses()}.
     */
    @Override
    @Deprecated
    @XmlElement(name = "address")
    public Address getAddress() {
        return LegacyPropertyAdapter.getSingleton(getAddresses(), Address.class, null, DefaultContact.class, "getAddress");
    }

    /**
     * Sets the physical and email address at which the organization or individual may be contacted.
     * This method delegates to {@link #setAddresses(Collection)}.
     *
     * @param newValue The new address, or {@code null} if none.
     *
     * @deprecated As of ISO 19115:2014, replaced by {@link #setAddresses(Collection)}.
     */
    @Deprecated
    public void setAddress(final Address newValue) {
        setAddresses(LegacyPropertyAdapter.asCollection(newValue));
    }

    /**
     * Returns on-line information that can be used to contact the individual or organization.
     *
     * @return On-line information that can be used to contact the individual or organization.
     *
     * @since 0.5
     */
    @Override
    public Collection<OnlineResource> getOnlineResources() {
        return onlineResources = nonNullCollection(onlineResources, OnlineResource.class);
    }

    /**
     * Sets on-line information that can be used to contact the individual or organization.
     *
     * @param newValues The new online resources.
     *
     * @since 0.5
     */
    public void setOnlineResources(final Collection<? extends OnlineResource> newValues) {
        onlineResources = writeCollection(newValues, onlineResources, OnlineResource.class);
    }

    /**
     * Returns on-line information that can be used to contact the individual or organization.
     * This method returns the first {@link #getOnlineResources() online resource} element, or null if none.
     *
     * @return On-line information that can be used to contact the individual or organization, or {@code null}.
     *
     * @deprecated As of ISO 19115:2014, replaced by {@link #getOnlineResources()}.
     */
    @Override
    @Deprecated
    @XmlElement(name = "onlineResource")
    public OnlineResource getOnlineResource() {
        return LegacyPropertyAdapter.getSingleton(getOnlineResources(), OnlineResource.class, null, DefaultContact.class, "getOnlineResource");
    }

    /**
     * Sets on-line information that can be used to contact the individual or organization.
     * This method delegates to {@link #setOnlineResources(Collection)}.
     *
     * @param newValue The new online resource, or {@code null} if none.
     *
     * @deprecated As of ISO 19115:2014, replaced by {@link #setOnlineResources(Collection)}.
     */
    @Deprecated
    public void setOnlineResource(final OnlineResource newValue) {
        setOnlineResources(LegacyPropertyAdapter.asCollection(newValue));
    }

    /**
     * Returns the time period (including time zone) when individuals can contact the organization or individual.
     *
     * @return Time period when individuals can contact the organization or individual.
     */
    @Override
    @XmlElement(name = "hoursOfService")
    public Collection<InternationalString> getHoursOfService() {
        return hoursOfService = nonNullCollection(hoursOfService, InternationalString.class);
    }

    /**
     * Sets time period (including time zone) when individuals can contact the organization or individual.
     *
     * @param newValues The new hours of service.
     */
    public void setHoursOfService(final Collection<? extends InternationalString> newValues) {
        hoursOfService = writeCollection(newValues, hoursOfService, InternationalString.class);
    }

    /**
     * Returns supplemental instructions on how or when to contact the individual or organization.
     *
     * @return Supplemental instructions on how or when to contact the individual or organization, or {@code null}.
     */
    @Override
    @XmlElement(name = "contactInstructions")
    public InternationalString getContactInstructions() {
        return contactInstructions;
    }

    /**
     * Sets supplemental instructions on how or when to contact the individual or organization.
     *
     * @param newValue The new contact instructions, or {@code null} if none.
     */
    public void setContactInstructions(final InternationalString newValue) {
        checkWritePermission();
        contactInstructions = newValue;
    }

    /**
     * Type of the contact.
     * Returns {@code null} if none.
     *
     * @return Type of the contact, or {@code null} if none.
     *
     * @since 0.5
     */
    @Override
/// @XmlElement(name = "contactType")
    public InternationalString getContactType() {
        return contactType;
    }

    /**
     * Sets new type of the contact.
     *
     * @param newValue The new type of the contact.
     *
     * @since 0.5
     */
    public void setContactType(final InternationalString newValue) {
        checkWritePermission();
        contactType = newValue;
    }
}

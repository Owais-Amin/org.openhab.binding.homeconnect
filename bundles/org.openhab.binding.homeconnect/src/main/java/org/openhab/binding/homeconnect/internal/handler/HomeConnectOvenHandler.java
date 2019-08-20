/**
 * Copyright (c) 2018-2019 Contributors to the openHAB project
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.binding.homeconnect.internal.handler;

import static org.eclipse.smarthome.core.library.unit.SmartHomeUnits.*;
import static org.openhab.binding.homeconnect.internal.HomeConnectBindingConstants.*;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import javax.measure.IncommensurableException;
import javax.measure.UnconvertibleException;
import javax.measure.quantity.Temperature;
import javax.measure.quantity.Time;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.library.types.QuantityType;
import org.eclipse.smarthome.core.library.types.StringType;
import org.eclipse.smarthome.core.library.unit.ImperialUnits;
import org.eclipse.smarthome.core.library.unit.SIUnits;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.UnDefType;
import org.openhab.binding.homeconnect.internal.client.exception.CommunicationException;
import org.openhab.binding.homeconnect.internal.client.model.Option;
import org.openhab.binding.homeconnect.internal.client.model.Program;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jersey.repackaged.com.google.common.collect.ImmutableList;

/**
 * The {@link HomeConnectOvenHandler} is responsible for handling commands, which are
 * sent to one of the channels of a oven.
 *
 * @author Jonas Brüstel - Initial contribution
 */
@NonNullByDefault
public class HomeConnectOvenHandler extends AbstractHomeConnectThingHandler {

    private final Logger logger = LoggerFactory.getLogger(HomeConnectOvenHandler.class);

    private static final ImmutableList<String> ACTIVE_STATE = ImmutableList.of(OPERATION_STATE_DELAYED_START,
            OPERATION_STATE_RUN, OPERATION_STATE_PAUSE);
    private static final ImmutableList<String> INACTIVE_STATE = ImmutableList.of(OPERATION_STATE_INACTIVE,
            OPERATION_STATE_READY);

    public HomeConnectOvenHandler(Thing thing,
            HomeConnectDynamicStateDescriptionProvider dynamicStateDescriptionProvider) {
        super(thing, dynamicStateDescriptionProvider);
    }

    @Override
    protected void configureChannelUpdateHandlers(ConcurrentHashMap<String, ChannelUpdateHandler> handlers) {
        // register default update handlers
        handlers.put(CHANNEL_OPERATION_STATE, defaultOperationStateChannelUpdateHandler());
        handlers.put(CHANNEL_POWER_STATE, defaultPowerStateChannelUpdateHandler());
        handlers.put(CHANNEL_DOOR_STATE, defaultDoorStateChannelUpdateHandler());
        handlers.put(CHANNEL_REMOTE_CONTROL_ACTIVE_STATE, defaultRemoteControlActiveStateChannelUpdateHandler());
        handlers.put(CHANNEL_REMOTE_START_ALLOWANCE_STATE, defaultRemoteStartAllowanceChannelUpdateHandler());
        handlers.put(CHANNEL_SELECTED_PROGRAM_STATE, defaultSelectedProgramStateUpdateHandler());

        // register oven specific update handlers
        handlers.put(CHANNEL_SETPOINT_TEMPERATURE, (channelUID, client) -> {
            Program program = client.getSelectedProgram(getThingHaId());
            if (program != null && program.getKey() != null) {
                Optional<Option> option = program.getOptions().stream()
                        .filter(o -> o.getKey().equals(OPTION_SETPOINT_TEMPERATURE)).findFirst();
                if (option.isPresent()) {
                    updateState(channelUID,
                            new QuantityType<>(option.get().getValueAsInt(), mapTemperature(option.get().getUnit())));
                } else {
                    updateState(channelUID, UnDefType.NULL);
                }
            }
        });
        handlers.put(CHANNEL_DURATION, (channelUID, client) -> {
            Program program = client.getSelectedProgram(getThingHaId());
            if (program != null && program.getKey() != null) {
                Optional<Option> option = program.getOptions().stream().filter(o -> o.getKey().equals(OPTION_DURATION))
                        .findFirst();
                if (option.isPresent()) {
                    updateState(channelUID, new QuantityType<>(option.get().getValueAsInt(), SECOND));
                } else {
                    updateState(channelUID, UnDefType.NULL);
                }
            }
        });
        handlers.put(CHANNEL_ACTIVE_PROGRAM_STATE, (channelUID, client) -> {
            Program program = client.getActiveProgram(getThingHaId());
            if (program != null && program.getKey() != null) {
                updateState(channelUID, new StringType(mapStringType(program.getKey())));
                program.getOptions().forEach(option -> {
                    switch (option.getKey()) {
                        case OPTION_REMAINING_PROGRAM_TIME:
                            getThingChannel(CHANNEL_REMAINING_PROGRAM_TIME_STATE)
                                    .ifPresent(channel -> updateState(channel.getUID(),
                                            option.getValueAsInt() == 0 ? UnDefType.NULL
                                                    : new QuantityType<>(option.getValueAsInt(), SECOND)));
                            break;
                        case OPTION_PROGRAM_PROGRESS:
                            getThingChannel(CHANNEL_PROGRAM_PROGRESS_STATE)
                                    .ifPresent(channel -> updateState(channel.getUID(),
                                            option.getValueAsInt() == 100 ? UnDefType.NULL
                                                    : new QuantityType<>(option.getValueAsInt(), PERCENT)));
                            break;
                        case OPTION_ELAPSED_PROGRAM_TIME:
                            getThingChannel(CHANNEL_ELAPSED_PROGRAM_TIME)
                                    .ifPresent(channel -> updateState(channel.getUID(),
                                            new QuantityType<>(option.getValueAsInt(), SECOND)));
                            break;
                    }
                });
            } else {
                updateState(channelUID, UnDefType.NULL);
                resetProgramStateChannels();
            }
        });
    }

    @Override
    protected void configureEventHandlers(ConcurrentHashMap<String, EventHandler> handlers) {
        // register default SSE event handlers
        handlers.put(EVENT_DOOR_STATE, defaultDoorStateEventHandler());
        handlers.put(EVENT_REMOTE_CONTROL_ACTIVE, defaultBooleanEventHandler(CHANNEL_REMOTE_CONTROL_ACTIVE_STATE));
        handlers.put(EVENT_REMOTE_CONTROL_START_ALLOWED,
                defaultBooleanEventHandler(CHANNEL_REMOTE_START_ALLOWANCE_STATE));
        handlers.put(EVENT_SELECTED_PROGRAM, defaultSelectedProgramStateEventHandler());
        handlers.put(EVENT_REMAINING_PROGRAM_TIME, defaultRemainingProgramTimeEventHandler());
        handlers.put(EVENT_PROGRAM_PROGRESS, defaultProgramProgressEventHandler());
        handlers.put(EVENT_ELAPSED_PROGRAM_TIME, defaultElapsedProgramTimeEventHandler());

        // register oven specific SSE event handlers
        handlers.put(EVENT_OPERATION_STATE, event -> {
            defaultOperationStateEventHandler().handle(event);

            if (STATE_OPERATION_FINISHED.equals(event.getValue())) {
                getThingChannel(CHANNEL_PROGRAM_PROGRESS_STATE)
                        .ifPresent(c -> updateState(c.getUID(), new QuantityType<>(100, PERCENT)));
            }

            if (STATE_OPERATION_RUN.equals(event.getValue())) {
                getThingChannel(CHANNEL_PROGRAM_PROGRESS_STATE)
                        .ifPresent(c -> updateState(c.getUID(), new QuantityType<>(0, PERCENT)));
                getThingChannel(CHANNEL_ACTIVE_PROGRAM_STATE).ifPresent(c -> updateChannel(c.getUID()));
            }

            if (STATE_OPERATION_READY.equals(event.getValue())) {
                resetProgramStateChannels();
            }
        });
        handlers.put(EVENT_POWER_STATE, event -> {
            defaultPowerStateEventHandler().handle(event);

            if (!STATE_POWER_ON.equals(event.getValue())) {
                resetProgramStateChannels();
                getThingChannel(CHANNEL_SELECTED_PROGRAM_STATE).ifPresent(c -> updateState(c.getUID(), UnDefType.NULL));
                getThingChannel(CHANNEL_ACTIVE_PROGRAM_STATE).ifPresent(c -> updateState(c.getUID(), UnDefType.NULL));
                getThingChannel(CHANNEL_SETPOINT_TEMPERATURE).ifPresent(c -> updateState(c.getUID(), UnDefType.NULL));
                getThingChannel(CHANNEL_DURATION).ifPresent(c -> updateState(c.getUID(), UnDefType.NULL));
            }
            if (STATE_POWER_ON.equals(event.getValue())) {
                updateChannels();
            }
        });
        handlers.put(EVENT_ACTIVE_PROGRAM, event -> {
            defaultActiveProgramEventHandler().handle(event);

            if (event.getValue() == null) {
                resetProgramStateChannels();
            }
        });
        handlers.put(EVENT_OVEN_CAVITY_TEMPERATURE, event -> {
            getThingChannel(CHANNEL_OVEN_CURRENT_CAVITY_TEMPERATURE).ifPresent(channel -> updateState(channel.getUID(),
                    new QuantityType<>(event.getValueAsInt(), mapTemperature(event.getUnit()))));
        });

        handlers.put(EVENT_SETPOINT_TEMPERATURE, event -> {
            getThingChannel(CHANNEL_SETPOINT_TEMPERATURE).ifPresent(channel -> updateState(channel.getUID(),
                    new QuantityType<>(event.getValueAsInt(), mapTemperature(event.getUnit()))));
        });
        handlers.put(EVENT_DURATION, event -> {
            getThingChannel(CHANNEL_DURATION).ifPresent(
                    channel -> updateState(channel.getUID(), new QuantityType<>(event.getValueAsInt(), SECOND)));
        });
    }

    @Override
    public void handleCommand(@NonNull ChannelUID channelUID, @NonNull Command command) {
        if (isThingReadyToHandleCommand()) {
            super.handleCommand(channelUID, command);

            if (logger.isDebugEnabled()) {
                logger.debug("{}: {}", channelUID, command);
            }

            try {
                // start or stop program
                if (command instanceof StringType && CHANNEL_BASIC_ACTIONS_STATE.equals(channelUID.getId())) {
                    updateState(channelUID, new StringType(""));

                    if ("start".equalsIgnoreCase(command.toFullString())) {
                        String program = getClient().getSelectedProgram(getThingHaId()).getKey();
                        getClient().startProgram(getThingHaId(), program);
                    } else {
                        getClient().stopProgram(getThingHaId());
                    }
                }

                // set selected program of coffee maker
                if (command instanceof StringType && CHANNEL_SELECTED_PROGRAM_STATE.equals(channelUID.getId())) {
                    getClient().setSelectedProgram(getThingHaId(), command.toFullString());
                }

                // turn coffee maker on and standby
                if (command instanceof OnOffType && CHANNEL_POWER_STATE.equals(channelUID.getId())) {
                    getClient().setPowerState(getThingHaId(),
                            OnOffType.ON.equals(command) ? STATE_POWER_ON : STATE_POWER_STANDBY);
                }

                // set setpoint temperature
                if (command instanceof QuantityType && CHANNEL_SETPOINT_TEMPERATURE.equals(channelUID.getId())) {
                    @SuppressWarnings("unchecked")
                    QuantityType<Temperature> quantity = ((QuantityType<Temperature>) command);

                    try {
                        String value;
                        String unit;

                        if (quantity.getUnit().equals(SIUnits.CELSIUS)
                                || quantity.getUnit().equals(ImperialUnits.FAHRENHEIT)) {
                            unit = quantity.getUnit().toString();
                            value = String.valueOf(quantity.intValue());
                        } else {
                            logger.info("Converting target setpoint temperture from {}{} to °C value.",
                                    quantity.intValue(), quantity.getUnit().toString());
                            unit = "°C";
                            value = String.valueOf(
                                    quantity.getUnit().getConverterToAny(SIUnits.CELSIUS).convert(quantity).intValue());
                            logger.info("{}{}", value, unit);
                        }

                        if (logger.isDebugEnabled()) {
                            logger.debug("Set setpoint temperature to {} {}.", value, unit);
                        }

                        String operationState = getCurrentOperationState();
                        if (operationState != null
                                && (ACTIVE_STATE.contains(operationState) || INACTIVE_STATE.contains(operationState))) {
                            getClient().setProgramOptions(getThingHaId(), OPTION_SETPOINT_TEMPERATURE, value, unit,
                                    true, ACTIVE_STATE.contains(operationState));
                        }

                    } catch (IncommensurableException | UnconvertibleException e) {
                        logger.error("Could not set setpoint!", e.getMessage());
                    }
                }

                // set duration
                if (command instanceof QuantityType && CHANNEL_DURATION.equals(channelUID.getId())) {
                    @SuppressWarnings("unchecked")
                    QuantityType<Time> quantity = ((QuantityType<Time>) command);

                    try {
                        String value = String
                                .valueOf(quantity.getUnit().getConverterToAny(SECOND).convert(quantity).intValue());

                        if (logger.isDebugEnabled()) {
                            logger.debug("Set duration to {} seconds.", value);
                        }

                        String operationState = getCurrentOperationState();
                        if (operationState != null
                                && (ACTIVE_STATE.contains(operationState) || INACTIVE_STATE.contains(operationState))) {
                            getClient().setProgramOptions(getThingHaId(), OPTION_DURATION, value, "seconds", true,
                                    ACTIVE_STATE.contains(operationState));
                        }
                    } catch (IncommensurableException | UnconvertibleException e) {
                        logger.error("Could not set duration! error: {}", e.getMessage());
                    }
                }
            } catch (CommunicationException e) {
                logger.warn("Could not handle command {}. API communication problem! error: {}", command.toFullString(),
                        e.getMessage());
            }
        }
    }

    @Override
    public String toString() {
        return "HomeConnectOvenHandler [haId: " + getThingHaId() + "]";
    }

    private void resetProgramStateChannels() {
        logger.debug("Resetting active program channel states");
        getThingChannel(CHANNEL_REMAINING_PROGRAM_TIME_STATE).ifPresent(c -> updateState(c.getUID(), UnDefType.NULL));
        getThingChannel(CHANNEL_PROGRAM_PROGRESS_STATE).ifPresent(c -> updateState(c.getUID(), UnDefType.NULL));
        getThingChannel(CHANNEL_ELAPSED_PROGRAM_TIME).ifPresent(c -> updateState(c.getUID(), UnDefType.NULL));
        getThingChannel(CHANNEL_OVEN_CURRENT_CAVITY_TEMPERATURE)
                .ifPresent(c -> updateState(c.getUID(), UnDefType.NULL));
    }
}
/*
 * This file is part of VeloAirChat, licensed under the Apache License 2.0.
 *
 *  Copyright (c) AirGalaxie/VeloAirChat contributors
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package de.airgalaxie.veloairchat.protocol;

import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class VeloAirChatBridgeProtocol {

    public static final String CHANNEL = "veloairchat:bridge";
    public static final int VERSION = 4;
    public static final int MAX_PACKET_BYTES = 32700;
    public static final int MAX_MESSAGE_CHARS = 1024;
    public static final int MAX_TEXT_CHARS = 8192;

    private VeloAirChatBridgeProtocol() {
    }

    public static byte[] encodeChatInput(@NotNull ChatEnvelope envelope) throws IOException {
        return write(MessageType.CHAT_INPUT, output -> {
            writeUuid(output, envelope.messageId());
            writeIdentityObservation(output, envelope.identity());
            writeSignedChatContext(output, envelope.signedChat());
            writeString(output, envelope.plainMessage(), MAX_MESSAGE_CHARS);
            output.writeLong(envelope.timestamp());
        });
    }

    public static ChatEnvelope decodeChatInput(byte[] packet) throws IOException {
        return read(packet, MessageType.CHAT_INPUT, input -> new ChatEnvelope(
                readUuid(input),
                readIdentityObservation(input),
                readSignedChatContext(input),
                readString(input, MAX_MESSAGE_CHARS),
                input.readLong()
        ));
    }

    public static byte[] encodeRenderedChat(@NotNull RenderedChatMessage message) throws IOException {
        return write(MessageType.RENDERED_CHAT, output -> {
            writeUuid(output, message.messageId());
            writeString(output, message.sourceServer(), 128);
            output.writeInt(message.recipients().size());
            for (UUID recipient : message.recipients()) {
                writeUuid(output, recipient);
            }
            writeString(output, message.miniMessage(), MAX_TEXT_CHARS);
            output.writeBoolean(message.dynmapPublish());
            writeString(output, message.dynmapServer(), 128);
            writeString(output, message.dynmapName(), 256);
            writeString(output, message.dynmapMessage(), MAX_TEXT_CHARS);
        });
    }

    public static RenderedChatMessage decodeRenderedChat(byte[] packet) throws IOException {
        return read(packet, MessageType.RENDERED_CHAT, input -> {
            final UUID messageId = readUuid(input);
            final String sourceServer = readString(input, 128);
            final int recipientCount = input.readInt();
            if (recipientCount < 0 || recipientCount > 1000) {
                throw new IOException("Invalid recipient count: " + recipientCount);
            }
            final List<UUID> recipients = new ArrayList<>(recipientCount);
            for (int i = 0; i < recipientCount; i++) {
                recipients.add(readUuid(input));
            }
            final String miniMessage = readString(input, MAX_TEXT_CHARS);
            final boolean dynmapPublish = input.readBoolean();
            final String dynmapServer = readString(input, 128);
            final String dynmapName = readString(input, 256);
            final String dynmapMessage = readString(input, MAX_TEXT_CHARS);
            return new RenderedChatMessage(messageId, sourceServer, recipients, miniMessage,
                    dynmapPublish, dynmapServer, dynmapName, dynmapMessage);
        });
    }

    public static byte[] encodePlaceholderRequest(@NotNull PlaceholderRequest request) throws IOException {
        return write(MessageType.PLACEHOLDER_REQUEST, output -> {
            writeUuid(output, request.requestId());
            writeUuid(output, request.playerId());
            writeString(output, request.template(), MAX_TEXT_CHARS);
        });
    }

    public static PlaceholderRequest decodePlaceholderRequest(byte[] packet) throws IOException {
        return read(packet, MessageType.PLACEHOLDER_REQUEST, input -> new PlaceholderRequest(
                readUuid(input), readUuid(input), readString(input, MAX_TEXT_CHARS)
        ));
    }

    public static byte[] encodePlaceholderResponse(@NotNull PlaceholderResponse response) throws IOException {
        return write(MessageType.PLACEHOLDER_RESPONSE, output -> {
            writeUuid(output, response.requestId());
            writeString(output, response.result(), MAX_TEXT_CHARS);
        });
    }

    public static PlaceholderResponse decodePlaceholderResponse(byte[] packet) throws IOException {
        return read(packet, MessageType.PLACEHOLDER_RESPONSE, input -> new PlaceholderResponse(
                readUuid(input), readString(input, MAX_TEXT_CHARS)
        ));
    }

    public static MessageType messageType(byte[] packet) throws IOException {
        if (packet.length > MAX_PACKET_BYTES) {
            throw new IOException("Packet too large: " + packet.length);
        }
        try (DataInputStream input = new DataInputStream(new ByteArrayInputStream(packet))) {
            final int version = input.readInt();
            if (version != VERSION) {
                throw new IOException("Unsupported protocol version: " + version);
            }
            return readEnum(input, MessageType.class);
        }
    }

    private static byte[] write(MessageType type, PacketWriter writer) throws IOException {
        try (ByteArrayOutputStream buffer = new ByteArrayOutputStream();
             DataOutputStream output = new DataOutputStream(buffer)) {
            output.writeInt(VERSION);
            output.writeUTF(type.name());
            writer.write(output);
            output.flush();
            final byte[] packet = buffer.toByteArray();
            if (packet.length > MAX_PACKET_BYTES) {
                throw new IOException("Packet too large: " + packet.length);
            }
            return packet;
        }
    }

    private static <T> T read(byte[] packet, MessageType expectedType, PacketReader<T> reader) throws IOException {
        if (packet.length > MAX_PACKET_BYTES) {
            throw new IOException("Packet too large: " + packet.length);
        }
        try (DataInputStream input = new DataInputStream(new ByteArrayInputStream(packet))) {
            final int version = input.readInt();
            if (version != VERSION) {
                throw new IOException("Unsupported protocol version: " + version);
            }
            final MessageType type = readEnum(input, MessageType.class);
            if (type != expectedType) {
                throw new IOException("Unexpected message type: " + type);
            }
            return reader.read(input);
        }
    }

    private static void writeUuid(DataOutputStream output, UUID uuid) throws IOException {
        output.writeLong(uuid.getMostSignificantBits());
        output.writeLong(uuid.getLeastSignificantBits());
    }

    private static void writeSignedChatContext(DataOutputStream output, SignedChatContext context)
            throws IOException {
        output.writeUTF(context.state().name());
        writeString(output, context.adapter(), 128);
        writeString(output, context.originalPlainMessage(), MAX_MESSAGE_CHARS);
        writeString(output, context.signatureFingerprint(), 128);
        output.writeLong(context.signedTimestamp());
        output.writeBoolean(context.chatSessionEvidencePresent());
        writeString(output, context.publicKeyFingerprint(), 128);
    }

    private static void writeIdentityObservation(DataOutputStream output, ChatIdentityObservation identity)
            throws IOException {
        writeUuid(output, identity.connectionUuid());
        writeString(output, identity.observedName(), 64);
        writeString(output, identity.adapter(), 128);
    }

    private static ChatIdentityObservation readIdentityObservation(DataInputStream input) throws IOException {
        try {
            return new ChatIdentityObservation(readUuid(input), readString(input, 64), readString(input, 128));
        } catch (IllegalArgumentException exception) {
            throw new IOException("Invalid chat identity observation", exception);
        }
    }

    private static SignedChatContext readSignedChatContext(DataInputStream input) throws IOException {
        try {
            return new SignedChatContext(
                    readEnum(input, SignedChatState.class),
                    readString(input, 128),
                    readString(input, MAX_MESSAGE_CHARS),
                    readString(input, 128),
                    input.readLong(),
                    input.readBoolean(),
                    readString(input, 128)
            );
        } catch (IllegalArgumentException exception) {
            throw new IOException("Invalid signed-chat context", exception);
        }
    }

    private static UUID readUuid(DataInputStream input) throws IOException {
        return new UUID(input.readLong(), input.readLong());
    }

    private static void writeString(DataOutputStream output, String value, int maxChars) throws IOException {
        if (value == null || value.length() > maxChars) {
            throw new IOException("Invalid string length");
        }
        final byte[] encoded = value.getBytes(StandardCharsets.UTF_8);
        if (encoded.length > MAX_PACKET_BYTES) {
            throw new IOException("Encoded string too large");
        }
        output.writeInt(encoded.length);
        output.write(encoded);
    }

    private static String readString(DataInputStream input, int maxChars) throws IOException {
        final int length = input.readInt();
        if (length < 0 || length > MAX_PACKET_BYTES) {
            throw new IOException("Invalid string byte length: " + length);
        }
        final byte[] encoded = input.readNBytes(length);
        if (encoded.length != length) {
            throw new IOException("Truncated string");
        }
        final String value = new String(encoded, StandardCharsets.UTF_8);
        if (value.length() > maxChars) {
            throw new IOException("String too long");
        }
        return value;
    }

    private static <E extends Enum<E>> E readEnum(DataInputStream input, Class<E> type) throws IOException {
        try {
            return Enum.valueOf(type, input.readUTF());
        } catch (IllegalArgumentException exception) {
            throw new IOException("Unknown enum value", exception);
        }
    }

    public enum MessageType {
        CHAT_INPUT,
        RENDERED_CHAT,
        PLACEHOLDER_REQUEST,
        PLACEHOLDER_RESPONSE
    }

    @FunctionalInterface
    private interface PacketWriter {
        void write(DataOutputStream output) throws IOException;
    }

    @FunctionalInterface
    private interface PacketReader<T> {
        T read(DataInputStream input) throws IOException;
    }
}

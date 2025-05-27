# Enhanced File Upload Logging Documentation

## Overview

The chat application now includes comprehensive logging for file uploads via WebSocket, showing detailed progress information for each chunk received with percentage completion, transfer rates, and time estimates.

## Features

### 1. Enhanced Chunk Reception Logging

Each chunk received is logged with detailed progress information:

```
WEBSOCKET: 📦 CHUNK RECEIVED [=====>    ] 25.00% | File: example.pdf | Chunk: 5/20 | Bytes: 1.25 MB of 5.00 MB (25.00%) | Rate: 2.50 MB/s | ETA: 1s | ID: example-123-20-1234567890
```

### 2. Upload Start Notification

When the first chunk is received, a startup message is logged:

```
WEBSOCKET: 🚀 STARTING FILE UPLOAD - ID: example-123-20-1234567890 | File: example.pdf | Size: 5.00 MB | Chunks: 20 | User: john_doe
```

### 3. Milestone Progress Tracking

Key milestones are logged for important progress points:

```
WEBSOCKET: 🎯 MILESTONE REACHED - 50.00% complete for upload example-123-20-1234567890 | 2.50 MB of 5.00 MB transferred at 2.50 MB/s
```

### 4. Upload Completion

When the file upload is complete:

```
WEBSOCKET: ✅ FILE UPLOAD COMPLETE - ID: example-123-20-1234567890 | File: example.pdf | Path: uploads/documents/20241201-143022-example-a1b2c3d4.pdf
```

## Log Components

### Progress Bar
- Visual representation: `[=====>    ]`
- 20-character width with `=` for completed, `>` for current position, and spaces for remaining

### Percentage Progress
- **Chunk Progress**: Based on chunks received vs total chunks
- **Bytes Progress**: Based on actual bytes received vs total file size

### Transfer Rate
- Automatically formatted in appropriate units (B/s, KB/s, MB/s)
- Calculated from upload start time to current time

### Time Estimates
- **ETA**: Estimated time remaining based on current transfer rate
- Formatted in seconds, minutes, or hours as appropriate

### File Size Formatting
- Human-readable format (B, KB, MB, GB, TB)
- Precise to 2 decimal places

## Configuration

### Logging Levels

The logging is configured in `application.yml`:

```yaml
logging:
  level:
    "[com.chatapp.websocket.BinaryFileController]": INFO
    "[com.chatapp.service.FileChunkService]": INFO
```

### Upload Tracking

The system tracks:
- **Upload Start Times**: For calculating transfer rates and time estimates
- **Received Bytes**: For accurate progress calculation
- **Upload IDs**: For correlating chunks across the upload process

## Implementation Details

### Upload ID Generation

Each upload gets a unique ID format:
```
{sanitized_filename}-{user_id}-{total_chunks}-{timestamp}
```

### Chunk Size Estimation

Base64 encoded chunk data is estimated to be ~75% of the original binary size for progress calculation.

### Progress Calculation

- **Chunk Progress**: `(current_chunk / total_chunks) * 100`
- **Bytes Progress**: `(received_bytes / total_file_size) * 100`
- **Transfer Rate**: `received_bytes / elapsed_seconds`

### Milestone Logging

Milestones are logged for:
- First chunk (chunk 1)
- Last chunk (final chunk)
- Every 10% progress interval
- Key progress points

## Benefits

1. **Real-time Monitoring**: See upload progress in real-time through logs
2. **Performance Tracking**: Monitor transfer rates and identify bottlenecks
3. **Debugging Support**: Detailed information for troubleshooting upload issues
4. **User Experience**: Better understanding of upload performance
5. **System Monitoring**: Track file upload patterns and performance metrics

## Example Log Sequence

```
2024-12-01 14:30:22 INFO  BinaryFileController : WEBSOCKET: 🚀 STARTING FILE UPLOAD - ID: video-mp4-456-100-1733058622000 | File: demo_video.mp4 | Size: 50.00 MB | Chunks: 100 | User: alice_smith

2024-12-01 14:30:22 INFO  BinaryFileController : WEBSOCKET: 📦 CHUNK RECEIVED [>         ] 1.00% | File: demo_video.mp4 | Chunk: 1/100 | Bytes: 512.00 KB of 50.00 MB (1.00%) | Rate: 512.00 KB/s | ETA: 1m 37s | ID: video-mp4-456-100-1733058622000

2024-12-01 14:30:25 INFO  BinaryFileController : WEBSOCKET: 📦 CHUNK RECEIVED [==>       ] 10.00% | File: demo_video.mp4 | Chunk: 10/100 | Bytes: 5.00 MB of 50.00 MB (10.00%) | Rate: 1.67 MB/s | ETA: 27s | ID: video-mp4-456-100-1733058622000

2024-12-01 14:30:25 INFO  BinaryFileController : WEBSOCKET: 🎯 MILESTONE REACHED - 10.00% complete for upload video-mp4-456-100-1733058622000 | 5.00 MB of 50.00 MB transferred at 1.67 MB/s

2024-12-01 14:30:40 INFO  BinaryFileController : WEBSOCKET: 📦 CHUNK RECEIVED [=========>] 50.00% | File: demo_video.mp4 | Chunk: 50/100 | Bytes: 25.00 MB of 50.00 MB (50.00%) | Rate: 1.39 MB/s | ETA: 18s | ID: video-mp4-456-100-1733058622000

2024-12-01 14:30:58 INFO  BinaryFileController : WEBSOCKET: 📦 CHUNK RECEIVED [==========] 100.00% | File: demo_video.mp4 | Chunk: 100/100 | Bytes: 50.00 MB of 50.00 MB (100.00%) | Rate: 1.32 MB/s | ETA: 0s | ID: video-mp4-456-100-1733058622000

2024-12-01 14:30:58 INFO  BinaryFileController : WEBSOCKET: ✅ FILE UPLOAD COMPLETE - ID: video-mp4-456-100-1733058622000 | File: demo_video.mp4 | Path: uploads/video/20241201-143058-demo_video-f9e8d7c6.mp4
```

## Troubleshooting

### Common Issues

1. **Missing Progress Logs**: Check logging level configuration
2. **Incorrect Progress**: Verify chunk size estimation and file size accuracy
3. **Performance Issues**: Monitor transfer rates in logs to identify bottlenecks

### Log Analysis

Use the upload ID to correlate all log entries for a specific file upload across the entire process.

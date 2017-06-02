--[[
    Licensed under the LICENSE.
    Copyright 2017, Sony Mobile Communications Inc.
  ]]

-- Push the job in the queue
redis.call("lpush", KEYS[1], ARGV[1])

-- Increment the number of jobs pushed
redis.call("hincrby", "queues_lenght", KEYS[1], ARGV[2])

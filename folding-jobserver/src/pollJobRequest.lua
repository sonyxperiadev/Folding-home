--[[
    Licensed under the LICENSE.
    Copyright 2017, Sony Mobile Communications Inc.
  ]]

local jobrequest = redis.call("lindex", KEYS[1], -1)

if jobrequest then
	local jobrequestDecoded = cjson.decode(jobrequest)
	local remaining = jobrequestDecoded["remaining"]

	local queue_lenght = redis.call("hincrby", "queues_lenght", KEYS[1], -1)

	if queue_lenght <= 0 then
		redis.call("hdel", "queues_lenght", KEYS[1])
	end

	if remaining > 1 then
		remaining = remaining - 1
		jobrequestDecoded["remaining"] = remaining
		local jobrequestEncoded = cjson.encode(jobrequestDecoded)
		redis.call("lset", KEYS[1], -1, jobrequestEncoded)
	else
		redis.call("rpop", KEYS[1])
	end
end

return jobrequest
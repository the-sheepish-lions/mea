#!/usr/bin/ruby

if ARGV.count < 1
  puts "USAGE: #$0 SCHEMA"
  exit 1
end

text = IO.read(ARGV[0])

def ids(text)
  ns = []
  text.gsub(/-\d{7,8}/) do |m|
    ns << m
  end
  ns
end

ids(text).zip((-151..-1).to_a.reverse).each do |id|
  text.sub!(id[0], id[1].to_s)
end

print text

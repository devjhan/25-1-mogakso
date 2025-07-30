from src.app.events.event_data import EventData

class _EventHook:
    def __init__(self):
        self._handlers = []

    def add(self, handler):
        if handler not in self._handlers:
            self._handlers.append(handler)
        return self

    def remove(self, handler):
        if handler in self._handlers:
            self._handlers.remove(handler)
        return self

    def fire(self, event_payload: EventData):
        for handler in self._handlers:
            try:
                handler(event_payload)
            except Exception as e:
                print(e)

    __iadd__ = add
    __isub__ = remove
    __call__ = fire

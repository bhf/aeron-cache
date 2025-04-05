import Logger from 'js-logger';

export function getLogger(name: string) {
    // eslint-disable-next-line react-hooks/rules-of-hooks
    Logger.useDefaults({formatter: function (messages, context) {
            // prefix each log message with a timestamp.
            const prefix = new Date().toUTCString()+" :: "+context.name+" :: "
            messages.unshift(prefix);
        },});

    return Logger.get(name);
}